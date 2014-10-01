package org.backuity.puppet

sealed trait Version extends Ordered[Version] {
  def isGreaterOrEquals(other: Version) : Boolean = {
    Version.isGreaterOrEquals(this, other)
  }
  def isCompatibleWith(other: Version) : Boolean = {
    Version.isCompatibleWith(this, other)
  }
  def compare(other: Version) : Int = {
    if( isGreaterOrEquals(other) ) {
      if( other.isGreaterOrEquals(this)) {
        0
      } else {
        1
      }
    } else {
      -1
    }
  }
}

object Version {

  object Latest extends Version

  case class MajorMinorBugFix(major: Int, minor: Int, bugfix: Int) extends Version

  /** @return true if `a` is greater or equals `b` */
  def isGreaterOrEquals(a: Version, b: Version) : Boolean = {
    (a,b) match {
      case (Latest, Latest)               => true
      case (Latest, _ : MajorMinorBugFix) => true
      case (_ : MajorMinorBugFix, Latest) => false
      case (a : MajorMinorBugFix, b : MajorMinorBugFix) =>
        if( a.major != b.major ) {
          a.major > b.major
        } else {
          if( a.minor != b.minor ) {
            a.minor > b.minor
          } else {
            a.bugfix >= b.bugfix
          }
        }
    }
  }

  def isCompatibleWith(a: Version, b: Version) : Boolean = {
    (a,b) match {
      case (Latest,Latest) => true
      case (Latest, _ : MajorMinorBugFix) => false
      case (_ : MajorMinorBugFix, Latest) => false
      case (a : MajorMinorBugFix, b : MajorMinorBugFix) => a.major == b.major
    }
  }

  def apply(ref: Option[String]) : Version = ref match {
    case None => Latest
    case Some(r) => Version(r)
  }

  // take everything after the first digit
  val VersionRegex = """[^0-9]*([0-9].*)""".r

  /**
   * Accept 1, 2 or 3 '.' separated digits versions, which can be prefixed by any number of chars.
   *
   * Example :
   *
   *   - `puppet-module-installer_1.2.0`
   *   - `v1.2`
   *   - `6`
   */
  def apply(versionString: String) : Version = {
    def fail(msg: String) : Nothing = {
      throw new IllegalArgumentException(s"Cannot parse $versionString : $msg")
    }

    def toInt(value: String, name: String): Int = {
      try {
        value.toInt
      } catch {
        case _ : NumberFormatException =>
          fail(s"$name '$value' is not a number")
      }
    }

    def toMajor(value: String) = toInt(value, "major")
    def toMinor(value: String) = toInt(value, "minor")

    VersionRegex.findFirstMatchIn(versionString).map( _.group(1)) match {
      case None => fail("cannot find a digit")
      case Some(digits) =>
        val nbDot = digits.count( _ == '.')
        nbDot match {
          case 0 => MajorMinorBugFix(toMajor(digits), 0, 0)
          case 1 =>
            val Array(major,minor) = digits.split("\\.")
            MajorMinorBugFix(toMajor(major), toMinor(minor), 0)

          case 2 =>
            val Array(major,minor,bugfix) = digits.split("\\.")
            MajorMinorBugFix(toMajor(major), toMinor(minor), toInt(bugfix, "bugfix"))

          case _ => fail("too many elements, expected 1, 2 or 3 '.' separated digits")
        }
    }
  }
}
