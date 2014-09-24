package org.backuity.puppet

import java.io.File
import java.net.URL
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicInteger
import java.util.jar.Manifest

import org.apache.commons.io.{FileUtils, IOUtils}

object ModuleInstaller {

  def main(args: Array[String]) {
    args.headOption match {
        case Some("--version") => showVersion()
        case Some("-v") => run(verbose = true)
        case _ => run(verbose = false)
    }
  }

  private def toManifest(url: URL) : Manifest = {
    new Manifest(url.openStream())
  }

  def showVersion(): Unit = {
    import scala.collection.JavaConversions._
    val manifests: List[URL] = this.getClass.getClassLoader.getResources("META-INF/MANIFEST.MF").toList
    manifests.map(toManifest).find( _.getMainAttributes.getValue("Implementation-Title") == "puppet-module-installer") match {
      case None => println("Unknown version")
      case Some(manifest) => println(manifest.getMainAttributes.getValue("Implementation-Version"))
    }
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

  private var modules : Set[String] = Set.empty[String]

  private val pool = Executors.newFixedThreadPool(6)
  private val done = new Semaphore(0)
  private val filesToProcess = new AtomicInteger(0)

  private def installFromGit(name: String, gitUri: String, ref: Option[String]) {
    if( ! isModulePresent(name) ) {
      println(s"> ${bold(name)} in $modulesDir from ${gitUri}${ref.map(r => " ref: " + r).getOrElse("")}")
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
    val modules = PuppetfileParser.parse(puppetFileContent).modules
    println(s"Found ${modules.size} modules in $puppetFile")
    filesToProcess.getAndAdd(modules.size)
    for ((name, module) <- modules) {
      module match {
        case ForgeModule(version) =>
          debug(yellow(s"Forge module $name has been ignored - forge modules are not supported."))
          decrementFilesToProcess()

        case GitModule(uri, ref) =>
          pool.execute(new Runnable {
            override def run(): Unit = {
              try {
                installFromGit(name, uri, ref)
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
  private def isModulePresent(name: String) : Boolean = {
    modules.synchronized {
      if( !modules.contains(name) ) {
        modules += name
        false
      } else {
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
    debug(blue(dir + " $ " + cmd))
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

  val bold = "1"
  val yellow = "33"
  val blue = "34"

  private def colorize(msg: String, codes : String*) : String = {
    "\u001b[" + codes.mkString(";") + "m" + msg + "\u001b[m"
  }

  private def bold(msg: String) : String = {
    colorize(msg, bold)
  }

  private def blue(msg: String) : String = colorize(msg, blue)
  private def yellow(msg: String) : String = colorize(msg, yellow)
}