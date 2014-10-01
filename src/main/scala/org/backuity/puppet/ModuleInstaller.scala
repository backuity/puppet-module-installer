package org.backuity.puppet

import java.nio.file.{Files, Path, Paths}
import java.util.concurrent._

import org.backuity.puppet.AnsiFormatter.FormattedHelper
import org.backuity.puppet.ConcurrentUtil.PimpExecutorService
import org.backuity.puppet.ModuleFetcher.FetchMode

object ModuleInstaller {

  def main(args: Array[String]) {

    var latest = false
    var verbose = false
    var showGraph = false
    var check = false

    val parser = new scopt.OptionParser[Unit]("puppet-module-installer") {
      opt[Unit]("latest").foreach { _ =>
        latest = true
      }
      opt[Unit]("verbose").abbr("v").foreach { _ =>
        verbose = true
      }
      opt[Unit]("check").text("Only check that the module versions are consistent, that is,\n" +
          "\twe cannot find 2 incompatible version (different major) of the same module").foreach { _ =>
        check = true
      }

      help("help")
      version("version")
      head("puppet-module-installer " + VersionUtil.versionFor("puppet-module-installer").getOrElse("Unknown version"))

      cmd("graph").text("Shows the graph of modules - do not install them").foreach { _ =>
        showGraph = true
      }
    }

    if (!parser.parse(args, ()).isDefined) {
      System.exit(2)
    }

    implicit val logger = new Logger.AnsiConsole(if( verbose ) LogLevel.Debug else LogLevel.Info)

    lazy val baseDir = Paths.get(System.getProperty("user.home")).resolve(".puppet-module-installer")

    lazy val repoBaseDir = baseDir.resolve("puppetfiles")

    lazy val puppetFile = {
      val file = Paths.get("Puppetfile")
      if( ! Files.isRegularFile(file)) {
        println("No Puppetfile found")
        System.exit(1)
      }
      file
    }

    lazy val shell = new ShellImpl
    lazy val git = new Git.Impl(shell)

    def fetchModules : Module.Graph = {
      val puppetFileRepo = new PuppetFileRepositoryImpl(repoBaseDir)
      val moduleFetcher = new ModuleFetcher(git, puppetFileRepo)

      val mode = if( latest ) FetchMode.LATEST else FetchMode.NORMAL
      moduleFetcher.buildModuleGraph(puppetFile, mode)
    }

    def run(): Unit = {
      val moduleGraph = fetchModules
      val modules = flattenModuleGraph(moduleGraph)

      val moduleDir = Paths.get("modules")
      new ModuleInstaller(moduleDir, git, shell).install(modules)
    }

    def doCheck(modules: Module.Graph, printFlatten : Boolean = false): Unit = {
      val flattenModules = flattenModuleGraph(modules)
      println("\nCheck OK - modules are consistent")
      if( printFlatten ) {
        println("\nFlatten:")
        for( (name, desc) <- flattenModules.toList.sortBy( _._1 ) ) {
          println(ansi"  $name(\bold{${desc.version}})" + (if(verbose) ansi" @ \blue{${desc.uri}}" else ""))
        }
      }
    }

    if( showGraph ) {
      val modules = fetchModules
      println(Module.showGraph(modules, withUri = verbose))
      if( check ) {
        doCheck(modules, printFlatten = true)
      }
    } else {
      if( check ) {
        doCheck(fetchModules)
      } else {
        run()
      }
    }
  }


  /**
   * Turns a graph of modules into a consistent list of modules.
   * A graph of modules is inconsistent if we can find 2 incompatible versions of the same module.
   * @throws IllegalArgumentException if the graph of modules isn't consistent.
   */
  def flattenModuleGraph(modules : Module.Graph): Map[String, Module.Description] = {
    var map = Map.empty[String,Module.Description]
    var modulePaths = Map.empty[String, Seq[String]]

    def add(module: Module, path: Seq[String]): Unit = {
      map += module.name -> module.description
      modulePaths += module.name -> path
      flatten(module.dependencies, path :+ module.name)
    }

    def flatten(modules: Module.Graph, path: Seq[String]): Unit = {
      for( module <- modules ) {
        map.get(module.name) match {
          case None => add(module, path)
          case Some(existing) =>
            if( existing.version.isCompatibleWith(module.version) ) {
              if( existing.version < module.version ) {
                add(module, path)
              }
            } else {
              val existingPath = (modulePaths(module.name) :+ module.name).mkString(" -> ")
              val newPath = (path :+ module.name).mkString(" -> ")
              throw new IllegalArgumentException(s"Incompatible version of module ${module.name}: $existingPath(${existing.version}), $newPath(${module.version})")
            }
        }
      }
    }

    flatten(modules, Seq.empty)

    map
  }
}

private class ModuleInstaller(modulesDir: Path, git: Git, shell: Shell)(implicit log: Logger) {

  private val pool = Executors.newFixedThreadPool(6)

  private def installFromGit(name: String, version: Version, gitUri: String, ref: Option[String]) {
    log.info(ansi"> \bold{$name}($version) in $modulesDir from ${gitUri}${ref.map(r => " ref: " + r).getOrElse("")}")
    val moduleDir = modulesDir.resolve(name)
    if( !Files.isDirectory(moduleDir) ) {
      Files.createDirectories(moduleDir)
    }
    cloneOrUpdate(gitUri, ref, moduleDir)
  }

  def install(modules: Map[String,Module.Description]) {
    for( (name, module) <- modules ) {
      pool.run {
        try {
          installFromGit(module.name, module.version, module.uri, module.tag)
        } catch {
          case t: Throwable =>
            Console.err.println(s"Cannot install $name from ${module.uri} ref:${module.tag.getOrElse("Latest")}")
            t.printStackTrace()
            // fatal failure... just terminate everything - we might want to add a mode that deletes
            // failing modules?
            System.exit(2)
        }
      }
    }
    pool.shutdown()
    pool.awaitTermination(1, TimeUnit.MINUTES)
  }

  private def cloneOrUpdate(gitUri: String, ref: Option[String], dir : Path) {
    if( Files.isDirectory(dir.resolve(".git")) ) {
      git.update(gitUri, ref, dir)
    } else {
      git.clone(gitUri, ref, dir)
    }
  }
}
