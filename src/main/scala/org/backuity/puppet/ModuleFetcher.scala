package org.backuity.puppet

import java.io.FileNotFoundException
import java.nio.file.{Files, Path}
import java.util

import org.backuity.puppet.ModuleFetcher.FetchMode
import org.backuity.puppet.PuppetFileRepository.{NotFound, Found, Missing}
import org.backuity.puppet.Puppetfile.{ForgeModule, GitModule}
import org.backuity.puppet.Version.{MajorMinorBugFix, Latest}

import scala.collection.concurrent.TrieMap

class ModuleFetcher(git : Git, puppetFileRepo : PuppetFileRepository)(implicit log: Logger) {

  def buildModuleGraph(puppetFile: Path, mode : FetchMode.FetchMode) : Set[Module] = {

    val moduleCache = new util.HashMap[(String, Version), Module]

    val latestVersionForMajorCache = TrieMap.empty[(String,Int), Option[String]]

    def latestTag(uri: String, forMajor: Int = -1) : Option[String] = {
      latestVersionForMajorCache.getOrElseUpdate((uri, forMajor),
        if( forMajor > -1 ) git.latestTag(uri, forMajor) else git.latestTag(uri))
    }

    def buildModuleGraph(puppetFile: Path, mode : FetchMode.FetchMode, parents: Seq[String]) : Set[Module] = {

      log.debug(s"Parsing $puppetFile ...")
      val puppetFileContent = new String(Files.readAllBytes(puppetFile), "UTF-8")
      val puppetFileModules = Puppetfile.parse(puppetFileContent).modules
      puppetFileModules.flatMap { case (name, module) =>

        if( parents.contains(name)) throw new ModuleFetcher.CyclicDependencyException(parents :+ name)

        module match {
          case _: ForgeModule =>
            log.warn(s"$puppetFile > forge module $name has been ignored - forge modules are not supported.")
            None

          case GitModule(uri, ref) =>

            val tag = mode match {
              case FetchMode.HEAD => None
              case FetchMode.NORMAL => ref
              case FetchMode.LATEST_FORCE => latestTag(uri)

              case FetchMode.LATEST_HEAD =>
                ref match {
                  case None => None
                  case Some(r) => latestTag(uri, forMajor = Version(r).major)
                }

              case FetchMode.LATEST =>
                ref match {
                  case None => latestTag(uri)
                  case Some(r) => latestTag(uri, forMajor = Version(r).major)
                }
            }

            val version = Version(tag)

            def downloadPuppetfile = git.downloadFile("Puppetfile", uri, tag)

            moduleCache.get((uri, version)) match {
              case null =>
                val dependencies = version match {
                  case Latest =>
                    // no caching when version is Latest
                    try {
                      buildModuleGraph(downloadPuppetfile, mode, parents :+ name)
                    } catch {
                      case _ : FileNotFoundException =>
                        log.debug(s"Cannot find Puppetfile in $uri $tag")
                        Set.empty[Module]
                    }

                  case mmbVersion : MajorMinorBugFix =>
                    puppetFileRepo.find(name, mmbVersion) match {
                      case Missing =>
                        log.debug(s"Puppetfile in $uri $tag is missing (cached info)")
                        Set.empty[Module]

                      case NotFound =>
                        try {
                          val modulePuppetFile = puppetFileRepo.add(name, mmbVersion, downloadPuppetfile)
                          buildModuleGraph(modulePuppetFile, mode, parents :+ name)
                        } catch {
                          case _ : FileNotFoundException =>
                            log.debug(s"Cannot find Puppetfile in $uri $tag")
                            puppetFileRepo.addMissing(name, mmbVersion)
                            Set.empty[Module]
                        }

                      case Found(modulePuppetFile) =>
                        buildModuleGraph(modulePuppetFile, mode, parents :+ name)
                    }
                }

                val module = Module(name, tag, uri, dependencies)
                moduleCache.put((uri, version), module)
                Some(module)

              case m => Some(m)
            }
        }
      }.toSet
    }

    buildModuleGraph(puppetFile, mode, Seq.empty)
  }
}

object ModuleFetcher {

  object FetchMode extends Enumeration {
    type FetchMode = Value

    /** Follow stricly the ref information found in the puppetfile */
    val NORMAL = Value

    /** Find the latest tag (for a major) */
    val LATEST = Value

    /** Find the latest tag, disregarding possible major information */
    val LATEST_FORCE = Value

    /** Find the latest tag (for a major) or use HEAD if no tag is specified */
    val LATEST_HEAD = Value

    /** Use HEAD for all modules */
    val HEAD = Value
  }

  case class CyclicDependencyException(chain: Seq[String]) extends
    RuntimeException("Cyclic dependency found : " + chain.mkString(" -> "))
}