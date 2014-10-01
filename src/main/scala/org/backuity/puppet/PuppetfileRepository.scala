package org.backuity.puppet

import java.nio.file.{Files, Path}

import org.backuity.puppet.PuppetFileRepository.{NotFound, Found, Missing, Entry}
import org.backuity.puppet.Version.MajorMinorBugFix

trait PuppetFileRepository {

  // TODO add missing Puppetfile

  /**
   * @return the added file
   */
  def add(name: String, version: MajorMinorBugFix, file: Path) : Path

  def addMissing(name: String, version: MajorMinorBugFix) : Unit

  def find(name: String, version: MajorMinorBugFix) : Entry
}

object PuppetFileRepository {
  sealed trait Entry

  case object Missing extends Entry
  case object NotFound extends Entry
  case class Found(path: Path) extends Entry
}

class PuppetFileRepositoryImpl(baseDir: Path)(implicit log : Logger) extends PuppetFileRepository {

  if( !Files.isDirectory(baseDir) ) {
    Files.createDirectories(baseDir)
  }

  // TODO locking

  private def pathFor(name: String, version: MajorMinorBugFix) : Path = {
    baseDir.resolve(name + "-" + version.toString)
  }

  private def missingPathFor(name: String, version: MajorMinorBugFix) : Path = {
    baseDir.resolve(s"missing.$name-${version.toString}")
  }

  override def add(name: String, version: MajorMinorBugFix, file: Path): Path = {
    val path = pathFor(name, version)
    log.debug(s"Adding $name $version to $path")
    Files.copy(file, path)
  }

  def addMissing(name: String, version: MajorMinorBugFix) : Unit = {
    Files.createFile(missingPathFor(name, version))
  }

  override def find(name: String, version: MajorMinorBugFix): Entry = {
    if( Files.exists(missingPathFor(name, version))) {
      Missing
    } else {
      pathFor(name, version) match {
        case exist if Files.exists(exist) => Found(exist)
        case _ => NotFound
      }
    }
  }
}