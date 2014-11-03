package org.backuity.puppet

import org.backuity.matchete.JunitMatchers
import org.backuity.puppet.Version.{Latest, MajorMinorBugFix}
import org.junit.Test

class VersionTest extends JunitMatchers {

  @Test
  def parseVersion(): Unit = {
    Version("1.3.2") must_== MajorMinorBugFix(1, 3, 2)
    Version("10.30.20") must_== MajorMinorBugFix(10, 30, 20)
    Version("puppet-module-installer_1.3.2") must_== MajorMinorBugFix(1, 3, 2)
    Version("puppet-module-installer_10.300.223") must_== MajorMinorBugFix(10, 300, 223)
    Version("1.3") must_== MajorMinorBugFix(1, 3, 0)
    Version("v1.3") must_== MajorMinorBugFix(1, 3, 0)
    Version("v10.30") must_== MajorMinorBugFix(10, 30, 0)

    Version("1") must_== MajorMinorBugFix(1, 0, 0)
    Version("some-version_1") must_== MajorMinorBugFix(1, 0, 0)
    Version("2") must_== MajorMinorBugFix(2, 0, 0)

    Version("puppet-apache2-8.2.13") must_== MajorMinorBugFix(8, 2, 13)
    Version("puppet-apache2-180.202.13") must_== MajorMinorBugFix(180, 202, 13)
    Version("puppet-apache23-8.2.13") must_== MajorMinorBugFix(8, 2, 13)
    Version("puppet-apache23-180.202.13") must_== MajorMinorBugFix(180, 202, 13)
    Version("puppet-apache2_8.2.13") must_== MajorMinorBugFix(8, 2, 13)
    Version("puppet-apache2_180.202.13") must_== MajorMinorBugFix(180, 202, 13)

    Version(None) must_== Latest
  }

  @Test
  def parseMustThrowMeaningfulException(): Unit = {
    Version("hmm?") must throwAn[IllegalArgumentException].withMessage("Cannot parse hmm? : no version information found")
    Version("1.x.2") must throwAn[IllegalArgumentException].withMessage("Cannot parse 1.x.2 : minor 'x' is not a number")
    Version("this-is-afunny28-prefx_1.x.2") must throwAn[IllegalArgumentException].withMessage(
      "Cannot parse this-is-afunny28-prefx_1.x.2 : minor 'x' is not a number")
    Version("1m.x.2") must throwAn[IllegalArgumentException].withMessage("Cannot parse 1m.x.2 : major '1m' is not a number")
    Version("apache2-1m.x.2") must throwAn[IllegalArgumentException].withMessage("Cannot parse apache2-1m.x.2 : major '1m' is not a number")
    Version("1.2.3x") must throwAn[IllegalArgumentException].withMessage("Cannot parse 1.2.3x : bugfix '3x' is not a number")
    Version("1.2.3-0") must throwAn[IllegalArgumentException].withMessage("Cannot parse 1.2.3-0 : bugfix '3-0' is not a number")
    Version("1.2.3.4") must throwAn[IllegalArgumentException].withMessage("Cannot parse 1.2.3.4 : too many elements, expected 1, 2 or 3 '.' separated digits")
  }

  @Test
  def compare(): Unit = {
    Version("1.3.10") must be_>(Version("1.3.9"))
    Version("1.3") must be_>(Version("1.2.9"))
    Version("1.3") must be_>(Version("1.2"))
    Version("1.3") must be_>(Version("1"))
    Version("2") must be_>(Version("1"))

    Version("1.3.9") must be_<(Version("1.3.10"))
    Version("1.2.9") must be_<(Version("1.3"))
    Version("1.1") must be_<(Version("1.3"))
    Version("12") must be_<(Version("12.3"))
    Version("2") must be_<(Version("3"))
  }
}
