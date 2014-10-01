package org.backuity.puppet

import java.io.FileNotFoundException
import java.nio.file.{Files, Path}
import java.util

import org.backuity.puppet.ModuleFetcher.FetchMode
import org.backuity.puppet.PuppetFileRepository.{NotFound, Found, Missing}
import org.backuity.puppet.Puppetfile.{ForgeModule, GitModule}
import org.backuity.puppet.Version.{MajorMinorBugFix, Latest}

class ModuleFetcher(git : Git, puppetFileRepo : PuppetFileRepository)(implicit log: Logger) {

  def buildModuleGraph(puppetFile: Path, mode : FetchMode.FetchMode) : Set[Module] = {

    val moduleCache = new util.HashMap[(String, Version), Module]
    val latestVersionCache = new util.HashMap[String, Option[String]]

    def latestTag(uri: String): Option[String] = {
      latestVersionCache.get(uri) match {
        case null =>
          val tag = git.latestTag(uri)
          latestVersionCache.put(uri, tag)
          tag

        case tag =>
          tag
      }
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

            val tag = if( mode == FetchMode.HEAD ) {
              None
            } else {
              if (mode == FetchMode.LATEST) latestTag(uri) else ref
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

    /** Find the latest tag */
    val LATEST = Value

    /** Use HEAD for all modules */
    val HEAD = Value
  }

  case class CyclicDependencyException(chain: Seq[String]) extends
    RuntimeException("Cyclic dependency found : " + chain.mkString(" -> "))
}