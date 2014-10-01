package org.backuity.puppet

import java.nio.file.{Files, Path}

import org.backuity.matchete.{EagerMatcher, MatcherSupport, Matcher}

trait FilesTestUtil extends MatcherSupport {

  implicit def pimpPath(path: Path) = new PimpPath(path)

  def contain(content: String) : Matcher[Path] = new EagerMatcher[Path] {
    protected def eagerCheck(path: Path): Unit = {
      if( ! path.exists ) {
        fail(path + " does not exist")
      }
      failIfDifferentStrings(path.readAll, content, path + " does not have the right content")
    }
    def description = "contain " + content
  }

  def exist = matcher[Path](
    description = "exist",
    validate = path => Files.exists(path),
    failureDescription = _.toString + " does not exist"
  )
}

class PimpPath(val path: Path) extends AnyVal {

  def /(child: String) : Path = path.resolve(child)

  /**
   * Write `content` as UTF-8
   */
  def write(content: String): Unit = {
    Files.write(path, content.getBytes("UTF-8"))
  }

  def exists : Boolean = Files.exists(path)

  def readAll : String = {
    new String(Files.readAllBytes(path), "UTF-8")
  }

  /**
   * For each pair of (name,content) write content as UTF-8 within `path`.
   */
  def addFiles(files: (String,String)*): Unit = {
    for( (name,content) <- files ) {
      Files.write(path.resolve(name), content.getBytes("UTF-8"))
    }
  }
}