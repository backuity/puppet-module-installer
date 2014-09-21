package org.backuity

import java.io.File
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicInteger

import org.apache.commons.io.{FileUtils, IOUtils}

object ModuleInstaller {

  def main(args: Array[String]) {
    val puppetFile: File = new File("Puppetfile")
    if( ! puppetFile.isFile) {
      println("No Puppetfile found")
      System.exit(1)
    }

    val verbose = args.length > 0 && args(0) == "-v"
    val moduleDir = new File("modules")
    new ModuleInstaller(moduleDir, verbose).installRoot(puppetFile)
  }
}

private class ModuleInstaller(modulesDir: File, verbose: Boolean = false) {

  private var modules : Set[String] = Set.empty[String]

  private val pool = Executors.newFixedThreadPool(6)
  private val done = new Semaphore(0)
  private val filesToProcess = new AtomicInteger(0)

  /** @return a Puppetfile from the new module */
  private def installFromGit(name: String, gitUri: String) {
    if( ! isModulePresent(name) ) {
      println(s"> ${bold(name)} in $modulesDir from $gitUri")
      val moduleDir = new File(modulesDir, name)
      if( !moduleDir.isDirectory && !moduleDir.mkdirs() ) {
        sys.error(s"Cannot create $name in $modulesDir")
      }
      clone(gitUri, moduleDir)
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

  private def install(puppetFile : File) {
    println(s"Parsing $puppetFile ...")
    val puppetFileContent = FileUtils.readFileToString(puppetFile)
    val modules = PuppetfileParser.parse(puppetFileContent)
    println(s"Found ${modules.size} modules in $puppetFile")
    filesToProcess.getAndAdd(modules.size)
    for ((name, uri) <- modules) yield {
      pool.execute(new Runnable {
        override def run(): Unit = {
          try {
            installFromGit(name, uri)
            if( filesToProcess.decrementAndGet() == 0 ) {
              done.release() // unlock the main thread to terminate
            }
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

  private def clone(gitUri: String, dir : File) {
    if( new File(dir, ".git").isDirectory ) {
      exec(s"git pull", dir)
    } else {
      exec(s"git clone $gitUri .", dir)
    }
  }

  private def exec(cmd: String, dir: File) {
    debug(grey(dir + " $ " + cmd))
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
  val grey = "30"

  private def colorize(msg: String, codes : String*) : String = {
    "\033[" + codes.mkString(";") + "m" + msg + "\033[m"
  }

  private def bold(msg: String) : String = {
    colorize(msg, bold)
  }

  private def grey(msg: String) : String = {
    colorize(msg, grey)
  }
}
