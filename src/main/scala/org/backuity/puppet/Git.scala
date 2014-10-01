package org.backuity.puppet

import java.io.FileNotFoundException
import java.nio.file.{StandardCopyOption, CopyOption, Files, Path}

trait Git {
  def update(source: String, ref: Option[String], destination: Path): Unit
  def clone(source: String, ref: Option[String], destination: Path) : Unit
  def currentBranch(dir: Path): String

  /** @throws FileNotFoundException */
  @throws(clazz = classOf[FileNotFoundException])
  def downloadFile(fileName: String, uri: String, tag: Option[String]): Path
  def lsRemoteTags(uri: String) : String

  /** @see [[Version]] */
  def latestTag(uri: String)(implicit log: Logger) : Option[String] = Git.latestVersion(lsRemoteTags(uri))
}

object Git {
  def latestVersion(gitOutput: String)(implicit log: Logger) : Option[String] = {
    gitOutput.split("\n").flatMap { line =>
      if (line.contains("refs/tags")) {
        val tag = line.split("refs/tags/")(1)
        if (!tag.endsWith("^{}") && tag.matches(".*[0-9]+.*")) {
          try {
            Some(tag,Version(tag))
          } catch {
            case e : IllegalArgumentException =>
              log.warn(e.getMessage)
              None
          }
        } else {
          None
        }
      } else {
        None
      }
    } match {
      case Array() => None
      case lst => Some(lst.maxBy( _._2)._1)
    }
  }

  class Impl(shell: Shell) extends Git {
    private val tmpDir = Files.createTempDirectory("pmi")

    def lsRemoteTags(uri: String) : String = {
      shell.exec("git ls-remote --tags " + uri)
    }

    def downloadFile(fileName: String, uri: String, tag: Option[String]) : Path = {
      val downloadDir = Files.createTempDirectory(tmpDir, "git")
      try {
        shell.exec(s"git archive --format tar --remote=$uri -o $fileName.tar ${tag.getOrElse("HEAD")} $fileName", downloadDir)
      } catch {
        case e @ CommandException(_,_,_,msg) =>
          if( msg.contains("path not found") ) {
            throw new FileNotFoundException(s"$fileName in $uri")
          } else {
            throw e
          }
      }
      shell.exec(s"tar -xf $fileName.tar", downloadDir)
      downloadDir.resolve(fileName)
    }

    def clone(source: String, ref: Option[String], destination: Path): Unit = {
      val branch = ref.map( r => " --branch " + r).getOrElse("")
      shell.exec(s"git clone$branch $source .", destination)
    }

    def currentBranch(dir: Path): String = {
      shell.exec("git rev-parse --abbrev-ref HEAD", dir).trim
    }

    def update(source: String, ref: Option[String], destination: Path): Unit = {
      ref match {
        case None =>
          if( currentBranch(destination) != "master" ) {
            shell.exec(s"git checkout master", destination)
          }
          shell.exec(s"git pull", destination)
        case Some(r) =>
          shell.exec(s"git fetch", destination)
          shell.exec(s"git checkout $r", destination)
      }
    }
  }
}
