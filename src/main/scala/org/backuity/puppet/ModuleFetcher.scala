package org.backuity.puppet

import java.io.FileNotFoundException
import java.nio.file.{Files, Path}
import java.util

import org.backuity.puppet.ModuleFetcher.{CyclicDependencyException, FetchMode}
import org.backuity.puppet.PuppetFileRepository.{NotFound, Found, Missing}
import org.backuity.puppet.Puppetfile.{ForgeModule, GitModule}
import org.backuity.puppet.Version.{MajorMinorBugFix, Latest}

import scala.collection.concurrent.TrieMap
import scala.util.control.NonFatal

class ModuleFetcher(git : Git, puppetFileRepo : PuppetFileRepository)(implicit log: Logger) {

  /** @throws [[ModuleFetcher.CyclicDependencyException]] */
  def buildModuleGraph(puppetFile: Path, mode : FetchMode.FetchMode, recurse: Boolean = true) : Set[Module] = {

    // TODO we might want to make this setting available on the CLI
    val abortOnError = false

    // Note : we want to make the following caches local as they depend on the fetch mode.

    val moduleCache = new util.HashMap[(String, Version), Module]

    val latestVersionForMajorCache = TrieMap.empty[(String,Int), Option[String]]

    def latestTag(uri: String, forMajor: Int = -1) : Option[String] = {
      latestVersionForMajorCache.getOrElseUpdate((uri, forMajor),
        if( forMajor > -1 ) git.latestTag(uri, forMajor) else git.latestTag(uri))
    }

    def tagFor(uri: String, ref: Option[String]): Option[String] = {
      mode match {
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
    }

    def buildModuleFromGit(name: String, uri: String, ref: Option[String], parents: Seq[String]): Module = {

      if (parents.contains(name)) throw new CyclicDependencyException(parents :+ name)

      val tag = tagFor(uri, ref)
      val version = Version(tag)

      def downloadPuppetfile = git.downloadFile("Puppetfile", uri, tag)

      def dependenciesForMajorMinorBugFixVersion(mmbVersion: MajorMinorBugFix): Set[Module] = {
        puppetFileRepo.find(name, mmbVersion) match {
          case Missing =>
            log.debug(s"Puppetfile in $uri $tag is missing (cached info)")
            Set.empty[Module]

          case NotFound =>
            try {
              val modulePuppetFile = puppetFileRepo.add(name, mmbVersion, downloadPuppetfile)
              buildModuleGraph(modulePuppetFile, mode, parents :+ name)
            } catch {
              case _: FileNotFoundException =>
                log.debug(s"Cannot find Puppetfile in $uri $tag")
                puppetFileRepo.addMissing(name, mmbVersion)
                Set.empty[Module]
            }

          case Found(modulePuppetFile) =>
            buildModuleGraph(modulePuppetFile, mode, parents :+ name)
        }
      }

      def dependenciesForLatestVersion: Set[Module] = {
        // no caching when version is Latest
        try {
          buildModuleGraph(downloadPuppetfile, mode, parents :+ name)
        } catch {
          case _: FileNotFoundException =>
            log.debug(s"Cannot find Puppetfile in $uri $tag")
            Set.empty[Module]
        }
      }

      def dependenciesForVersion(version: Version): Set[Module] = {
        try {
          version match {
            case Latest                       => dependenciesForLatestVersion
            case mmbVersion: MajorMinorBugFix => dependenciesForMajorMinorBugFixVersion(mmbVersion)
          }
        } catch {
          // let cyclic dependencies errors bubble up
          case NonFatal(err) if !abortOnError && !err.isInstanceOf[CyclicDependencyException] =>
            log.warn(s"Cannot figure out dependencies of module '$name' at $uri $tag : ${err.getMessage}")
            Set.empty[Module]
        }
      }

      def buildModule: Module = {
        val dependencies = if( recurse ) {
          dependenciesForVersion(version)
        } else {
          Set.empty[Module]
        }
        Module(name, tag, uri, dependencies)
      }

      def cacheModule(module: Module) : Module = {
        moduleCache.put((uri, version), module)
        module
      }

      moduleCache.get((uri, version)) match {
        case null => cacheModule(buildModule)
        case m    => m
      }
    }

    /** @throws [[CyclicDependencyException]] */
    def buildModuleGraph(puppetFile: Path, mode : FetchMode.FetchMode, parents: Seq[String]) : Set[Module] = {

      log.debug(s"Parsing $puppetFile ...")
      val puppetFileContent = new String(Files.readAllBytes(puppetFile), "UTF-8")
      val puppetFileModules = Puppetfile.parse(puppetFileContent).modules

      puppetFileModules.flatMap {
        case (name, _ : ForgeModule) =>
          log.warn(s"$puppetFile > forge module $name has been ignored - forge modules are not supported.")
          None

        case (name, GitModule(uri, ref)) =>
          Some(buildModuleFromGit(name, uri, ref, parents))
      }.toSet
    }

    buildModuleGraph(puppetFile, mode, parents = Seq.empty)
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