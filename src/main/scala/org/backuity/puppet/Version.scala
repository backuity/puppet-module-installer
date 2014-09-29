package org.backuity.puppet

sealed trait Version {
  def isGreaterOrEqual(other: Version) : Boolean = {
    Version.isGreaterOrEqual(this, other)
  }
  def isCompatibleWith(other: Version) : Boolean = {
    Version.isCompatibleWith(this, other)
  }
}

object Version {

  object Latest extends Version

  case class MajorMinorBugFix(major: Int, minor: Int, bugfix: Int) extends Version

  def isGreaterOrEqual(a: Version, b: Version) : Boolean = {
    false
  }

  def isCompatibleWith(a: Version, b: Version) : Boolean = {
    false
  }

  def apply(ref: Option[String]) : Version = ref match {
    case None => Latest
    case Some(r) =>
      val majorMinorBugFix = """([0-9]*)\.([0-9]*)\.([0-9]*)""".r
      try {
        val majorMinorBugFix(major, minor, bugfix) = r
        MajorMinorBugFix(major.toInt, minor.toInt, bugfix.toInt)
      } catch {
        case _ => null
      }
    }
}
