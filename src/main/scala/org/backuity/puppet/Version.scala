package org.backuity.puppet

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

  val MajorVersionRegex            = """([^\.]*[0-9]+)""".r // a major must contain a number
  val MajorMinorVersionRegex       = """([^\.]+)\.([^\.]+)""".r
  val MajorMinorBugFixVersionRegex = """([^\.]+)\.([^\.]+)\.([^\.]+)""".r

  // used for the 'too many elements' error message
  val NumberLeadingRegex           = """[^0-9]*([0-9].*)""".r

  val NextNumberLeadingRegex       = """.[^0-9]*([0-9].*)""".r


  /**
   * Accept 1, 2 or 3 '.' separated digits versions, which can be prefixed by any number of chars.
   *
   * Example :
   *
   *   - `puppet-module-installer_1.2.0`
   *   - `apache2_123.45.6`
   *   - `v1.2`
   *   - `6`
   * @throws IllegalArgumentException if versionString cannot be parsed into a Version
   */
  def apply(versionString: String) : MajorMinorBugFix = {
    def fail(msg: String) : Nothing = {
      throw new IllegalArgumentException(s"Cannot parse $versionString : $msg")
    }

    def toInt(value: String, name: String, retry: Boolean = false): Int = {

      def notANumber() = fail(s"$name '$value' is not a number")

      try {
        value.toInt
      } catch {
        case _ : NumberFormatException =>
          if( retry ) {
            try {
              val NextNumberLeadingRegex(number) = value
              toInt(number, name, retry)
            } catch {
              case _: MatchError => notANumber()
            }
          } else {
            notANumber()
          }
      }
    }

    def toMajor(value: String) = toInt(value, "major", retry = true)
    def toMinor(value: String) = toInt(value, "minor")

    versionString match {
      case MajorMinorBugFixVersionRegex(major,minor,bugfix) => MajorMinorBugFix(toMajor(major), toMinor(minor), toInt(bugfix, "bugfix"))
      case MajorMinorVersionRegex(major, minor)             => MajorMinorBugFix(toMajor(major), toMinor(minor), 0)
      case MajorVersionRegex(major)                         => MajorMinorBugFix(toMajor(major), 0, 0)
      case NumberLeadingRegex(content) if content.count( _ == '.') > 2 => fail("too many elements, expected 1, 2 or 3 '.' separated digits")
      case other => fail("no version information found")
    }
  }
}
