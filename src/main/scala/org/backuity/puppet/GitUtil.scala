package org.backuity.puppet

object GitUtil {

  // take everything after the first digit
  val VersionRegex = """[^0-9]*([0-9].*)""".r

  def latestVersion(gitOutput: String)(implicit log: Logger) : Option[Version] = {
    val name = ""
    gitOutput.split("\n").flatMap { line =>
      if (line.contains("refs/tags")) {
        val tag = line.split("refs/tags/")(1)
        if (!tag.endsWith("^{}")) {
          VersionRegex.findFirstMatchIn(tag).map( _.group(1))
        } else {
          None
        }
      } else {
        None
      }
    }.flatMap { versionString =>
      try {
        Some(Version(versionString))
      } catch {
        case e : IllegalArgumentException =>
          log.warn(e.getMessage)
          None
      }
    } match {
      case Array() => None
      case lst => Some(lst.max)
    }
  }
}
