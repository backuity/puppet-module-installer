package org.backuity.puppet

import org.backuity.matchete.JunitMatchers
import org.backuity.puppet.Version.{Latest, MajorMinorBugFix}
import org.junit.Test

class VersionTest extends JunitMatchers {

  @Test
  def parseVersion(): Unit = {
    Version("1.3.2") must_== MajorMinorBugFix(1, 3, 2)
    Version("1.3") must_== MajorMinorBugFix(1, 3, 0)

    Version("1") must_== MajorMinorBugFix(1, 0, 0)
    Version("2") must_== MajorMinorBugFix(2, 0, 0)

    Version(None) must_== Latest
  }

  @Test
  def parseMustThrowMeaningfulException(): Unit = {
    Version("1.x.2") must throwAn[IllegalArgumentException].withMessage("Cannot parse 1.x.2 : minor 'x' is not a number")
    Version("m.x.2") must throwAn[IllegalArgumentException].withMessage("Cannot parse m.x.2 : major 'm' is not a number")
    Version("1.2.3x") must throwAn[IllegalArgumentException].withMessage("Cannot parse 1.2.3x : bugfix '3x' is not a number")
    Version("1.2.3.4") must throwAn[IllegalArgumentException].withMessage("Cannot parse 1.2.3.4 : too many elements, expected 1, 2 or 3")
  }
}
