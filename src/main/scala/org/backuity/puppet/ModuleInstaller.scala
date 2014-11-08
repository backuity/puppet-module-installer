package org.backuity.puppet

import java.nio.file.{Files, Path, Paths}
import java.util.concurrent._

import org.backuity.puppet.AnsiFormatter.FormattedHelper
import org.backuity.puppet.ConcurrentUtil.PimpExecutorService
import org.backuity.puppet.ModuleFetcher.FetchMode

object ModuleInstaller {

  case class CliArgs(
    mode : FetchMode.FetchMode = FetchMode.NORMAL,
    verbose : Boolean = false,
    check : Boolean = false,
    command: String = "",
    onlyModules : List[String] = Nil
  ) {
    def command(cmd: String) : CliArgs = copy(command = cmd)
  }

  def parse(args: Array[String]) : CliArgs = {
    val parser = new scopt.OptionParser[CliArgs]("puppet-module-installer") {

      opt[Unit]("latest").text("Fetch the highest possible minor version of each module or\n" +
          "\tuse latest if no version information is available.")
          .action { (a,c) => c.copy( mode = FetchMode.LATEST ) }
      opt[Unit]("latest-force").text("Fetch the highest possible version of each module (disregard major information).")
          .action { (a,c) => c.copy( mode = FetchMode.LATEST_FORCE ) }
      opt[Unit]("latest-head").text("Fetch the highest possible minor version of each module or\n" +
          "\tuse HEAD if no version information is available.")
          .action { (a,c) => c.copy( mode = FetchMode.LATEST_HEAD ) }
      opt[Unit]("head").text("Use HEAD version for all modules.")
          .action { (a,c) => c.copy( mode = FetchMode.HEAD ) }

      opt[Unit]("verbose").abbr("v").action { (a,c) => c.copy( verbose = true ) }
      opt[Unit]("check").text("Only check that the module versions are consistent, that is,\n" +
          "\twe cannot find 2 incompatible version (different major) of the same module").action { (a,c) =>
        c.copy( check = true )
      }

      help("help")
      version("version")
      head("puppet-module-installer " + VersionUtil.versionFor("puppet-module-installer").getOrElse("Unknown version"))

      cmd("graph").text("Shows the graph of modules - do not install them").action { (a,c) => c.command("graph") }

      cmd("show").text("Shows the current modules. Dirty modules are shown in yellow.").action { (a,c) => c.command("show") }

      cmd("only").text("Update only subset of the modules").children(
        arg[String]("<module>...").unbounded().required().text("List of modules to update").action { (m, c) =>
          c.copy( onlyModules = c.onlyModules :+ m )
        }
      ).action { (a,c) => c.command("only") }
    }

    parser.parse(args, new CliArgs) match {
      case None => sys.exit(1);
      case Some(cliArgs) => cliArgs
    }
  }

  def main(args: Array[String]) {

    val cla = parse(args)

    implicit val logger = new Logger.AnsiConsole(if( cla.verbose ) LogLevel.Debug else LogLevel.Info)

    lazy val baseDir = Paths.get(System.getProperty("user.home")).resolve(".puppet-module-installer")
    lazy val repoBaseDir = baseDir.resolve("puppetfiles")
    lazy val moduleDir = Paths.get("modules")

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

    lazy val moduleAnalyzer = new ModuleAnalyzer(git)
    lazy val moduleInstaller = new ModuleInstaller(moduleDir, git, shell)

    def fetchModules : Module.Graph = {
      val puppetFileRepo = new PuppetFileRepositoryImpl(repoBaseDir)
      val moduleFetcher = new ModuleFetcher(git, puppetFileRepo)

      moduleFetcher.buildModuleGraph(puppetFile, cla.mode)
    }

    def run(): Unit = {
      val moduleGraph = fetchModules
      val modules = flattenModuleGraph(moduleGraph)
      moduleInstaller.install(modules)
    }

    def doCheck(modules: Module.Graph, printFlatten : Boolean = false): Unit = {
      val flattenModules = flattenModuleGraph(modules)
      println("\nCheck OK - modules are consistent")
      if( printFlatten ) {
        println("\nFlatten:")
        for( (name, desc) <- flattenModules.toList.sortBy( _._1 ) ) {
          println(ansi"  $name(\bold{${desc.version}})" + (if(cla.verbose) ansi" @ \blue{${desc.uri}}" else ""))
        }
      }
    }

    def localModules() : Set[LocalModule] = {
      moduleAnalyzer.analyze(moduleDir)
    }

    cla.command match {
      case "graph" =>
        val modules = fetchModules
        println(Module.showGraph(modules, withUri = cla.verbose))
        if( cla.check ) {
          doCheck(modules, printFlatten = true)
        }

      case "show" =>
        localModules().toList match {
          case Nil => println("No modules found.")
          case notEmpty => notEmpty.sortBy(_.name).foreach { m =>
            val version = m.version match {
              case None => ansi"\red{invalid}"
              case Some(v) => v.toString
            }
            val name = ansi"> ${m.name}(\bold{$version})"
            if( m.isDirty ) {
              println(ansi"\yellow{$name}")
            } else {
              println(name)
            }
          }
        }

      case "only" =>
        val flattenModules = flattenModuleGraph(fetchModules)
        for( onlyModule <- cla.onlyModules ) {
          flattenModules.get(onlyModule) match {
            case None => println(ansi"\red{\bold{$onlyModule} is not a known module.}")
            case Some(module) => moduleInstaller.install(module)
          }
        }

      case _ =>
        if( cla.check ) {
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

  def install(module: Module.Description, exitUponFailure: Boolean = false): Unit = {
    try {
      installFromGit(module.name, module.version, module.uri, module.tag)
    } catch {
      case t: Throwable =>
        Console.err.println(s"Cannot install ${module.name} from ${module.uri} ref:${module.tag.getOrElse("Latest")}")
        t.printStackTrace()
        // fatal failure... just terminate everything - we might want to add a mode that deletes
        // failing modules?
        if( exitUponFailure ) {
          System.exit(2)
        }
    }
  }

  def install(modules: Map[String,Module.Description]) {
    for( (name, module) <- modules ) {
      pool.run {
        install(module, exitUponFailure = true)
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
