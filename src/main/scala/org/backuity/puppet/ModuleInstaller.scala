package org.backuity.puppet

import java.io.File
import java.nio.file.Files
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicInteger

import org.apache.commons.io.{FileUtils, IOUtils}
import org.backuity.puppet.AnsiFormatter.FormattedHelper
import org.backuity.puppet.Puppetfile.{GitModule, ForgeModule}

object ModuleInstaller {

  case class Module(name: String, version: String, dependencies: Set[Module])

  trait Git {
    def lsRemoteTags(uri: String) : String
  }

  class GitImpl extends Git {
    def lsRemoteTags(uri: String) : String = {
      // exec("git ls-remote --tags " + uri)
      ""
    }
  }

  class ModuleFetcher(git : Git) {
    def buildModuleGraph(puppetFile: File, forceLatestVersion: Boolean) : Set[Module] = {
      println(s"Parsing $puppetFile ...")
      val puppetFileContent = FileUtils.readFileToString(puppetFile)
      val puppetFileModules = Puppetfile.parse(puppetFileContent).modules
      for( (name,module) <- puppetFileModules ) {
        module match {
          case _ : ForgeModule => println("Unsupported")
          case GitModule(uri, ref) =>
            ref match {
              case None => getLatestTag(name, uri)
              case Some(r) => if( forceLatestVersion ) getLatestTag(name, uri) else r
            }
        }
      }
      null
    }

    /** @return stdout */
    private def exec(cmd: String, dir: File = new File(".")): String = {
      ""
    }

    def getLatestTag(name: String, uri: String) : Option[Version] = {
      val gitOutput = git.lsRemoteTags(uri)
      GitUtil.latestVersion(gitOutput)(new AnsiConsoleLogger)
    }
  }

  def main(args: Array[String]) {
    args.headOption match {
      // TODO --validate by checking out only the Puppetfile
      //      see http://stackoverflow.com/questions/2466735/checkout-only-one-file-from-git

        case Some("--version") => showVersion()
        case Some("-v") => run(verbose = true)
        case _ => run(verbose = false)
    }
  }

  def showVersion(): Unit = {
    println( VersionUtil.versionFor("puppet-module-installer").getOrElse("Unknown version") )
  }

  def run(verbose: Boolean): Unit = {
      val puppetFile: File = new File("Puppetfile")
      if( ! puppetFile.isFile) {
          println("No Puppetfile found")
          System.exit(1)
      }

      val moduleDir = new File("modules")
      new ModuleInstaller(moduleDir, verbose).installRoot(puppetFile)
  }
}

private class ModuleInstaller(modulesDir: File, verbose: Boolean = false) {

  private val modules : collection.mutable.Map[String, Option[String]] = collection.mutable.Map.empty

  private val pool = Executors.newFixedThreadPool(6)
  private val done = new Semaphore(0)
  private val filesToProcess = new AtomicInteger(0)

  private def installFromGit(name: String, gitUri: String, ref: Option[String], origin: String) {
    if( ! isModulePresent(name, ref, origin) ) {
      println(ansi"> \bold{$name} in $modulesDir from ${gitUri}${ref.map(r => " ref: " + r).getOrElse("")}")
      val moduleDir = new File(modulesDir, name)
      if( !moduleDir.isDirectory && !moduleDir.mkdirs() ) {
        sys.error(s"Cannot create $name in $modulesDir")
      }
      clone(gitUri, ref, moduleDir)
      val puppetFile: File = new File(moduleDir, "Puppetfile")
      if (puppetFile.isFile) {
        install(puppetFile)
      }
    }
  }

  /** can only be called once */
  def installRoot(puppetFile: File) {
    try {
      install(puppetFile)
      if( done.tryAcquire(2, TimeUnit.MINUTES) ) {
        println("Done")
      } else {
        println("Timeout! Try again later...")
      }
    } finally {
      pool.shutdownNow()
    }
  }

  private def decrementFilesToProcess(): Unit = {
    if( filesToProcess.decrementAndGet() == 0 ) {
      done.release() // unlock the main thread to terminate
    }
  }

  private def install(puppetFile : File) {
    println(s"Parsing $puppetFile ...")
    val puppetFileContent = FileUtils.readFileToString(puppetFile)
    val modules = Puppetfile.parse(puppetFileContent).modules
    println(s"Found ${modules.size} modules in $puppetFile")
    filesToProcess.getAndAdd(modules.size)
    for ((name, module) <- modules) {
      module match {
        case ForgeModule(version) =>
          debug(ansi"\yellow{Forge module $name has been ignored - forge modules are not supported.}")
          decrementFilesToProcess()

        case GitModule(uri, ref) =>
          pool.execute(new Runnable {
            override def run(): Unit = {
              try {
                installFromGit(name, uri, ref, origin = puppetFile.toString)
                decrementFilesToProcess()
              } catch {
                case t : Throwable =>
                  Console.err.println(s"Cannot install $name from $uri")
                  t.printStackTrace()
                  // fatal failure... just terminate everything - we might want to add a mode that deletes
                  // failing modules?
                  System.exit(2)
              }
            }
          })
      }
    }
  }

  /** @return true if the module is currently being
    *         installed or has already been installed */
  private def isModulePresent(name: String, ref: Option[String], origin: String) : Boolean = {
    modules.synchronized {
      modules.get(name) match {
        case None =>
          modules.put( name, ref )
          false

        case Some(existing) =>
          if( ! Version(existing).isGreaterOrEquals(Version(ref)) ) {
            warn(ansi"Module \bold{$name} has been installed with version " +
              existing.getOrElse("LATEST") + s" but it is required by $origin with version " +
              ref.getOrElse("LATEST"))
          }
          true
      }
    }
  }

  private def clone(gitUri: String, ref: Option[String], dir : File) {
    if( new File(dir, ".git").isDirectory ) {
      ref match {
        case None => exec(s"git pull", dir)
        case Some(r) =>
          exec(s"git fetch", dir)
          exec(s"git checkout $r", dir)
      }
    } else {
      val branch = ref.map( r => " --branch " + r).getOrElse("")
      exec(s"git clone$branch $gitUri .", dir)
    }
  }

  private def exec(cmd: String, dir: File) {
    debug(ansi"\blue{$dir $$ $cmd}")
    val process = Runtime.getRuntime.exec(cmd, null, dir)
    process.waitFor() match {
      case 0 =>
      case error =>
        val errorStream = IOUtils.toString(process.getErrorStream)
        throw new RuntimeException(s"Command '$cmd' executed in $dir failed ($error):\n$errorStream")
    }
  }

  private def debug(msg: String) {
    if( verbose ) println(msg)
  }

  private def warn(msg: String) {
    println(ansi"\red{$msg}")
  }
}
