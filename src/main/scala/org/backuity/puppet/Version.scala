package org.backuity.puppet

import org.backuity.puppet.Version.{MajorMinorBugFix, Latest}

sealed trait Version extends Ordered[Version] {
  def isCompatibleWith(other: Version) : Boolean = {
    Version.isCompatibleWith(this, other)
  }

  override def compare(that: Version): Int = Version.versionOrdering.compare(this, that)
}

object Version {

  object Latest extends Version {
    override def toString = "HEAD"
  }

  implicit val versionOrdering = new Ordering[Version] {
    def compare(a: Version, b: Version): Int = {
      (a,b) match {
        case (Latest, Latest)               => 0
        case (Latest, _ : MajorMinorBugFix) => 1
        case (_ : MajorMinorBugFix, Latest) => -1
        case (a : MajorMinorBugFix, b : MajorMinorBugFix) =>
          majorMinorBugFixOrdering.compare(a, b)
      }
    }
  }

  implicit val majorMinorBugFixOrdering : Ordering[MajorMinorBugFix] = new Ordering[MajorMinorBugFix] {
    def compare(a: MajorMinorBugFix, b: MajorMinorBugFix): Int = {
      if( a.major != b.major ) {
        a.major.compareTo(b.major)
      } else {
        if( a.minor != b.minor ) {
          a.minor.compareTo(b.minor)
        } else {
          a.bugfix.compareTo(b.bugfix)
        }
      }
    }
  }

  case class MajorMinorBugFix(major: Int, minor: Int, bugfix: Int) extends Version {
    override def toString = s"$major.$minor.$bugfix"
  }

  def isCompatibleWith(a: Version, b: Version) : Boolean = {
    (a,b) match {
      case (Latest, _) | (_, Latest) => true
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
   * @throws IllegalArgumentException if versionString cannot be parsed into a Version
   */
  def apply(versionString: String) : MajorMinorBugFix = {
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
