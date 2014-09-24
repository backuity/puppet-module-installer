package org.backuity.puppet

import org.backuity.matchete.JunitMatchers
import org.junit.Test

class PuppetfileParserTest extends JunitMatchers {

  @Test
  def duplicatedModuleDefinitionsIsntAllowed() {
    PuppetfileParser.parse("""
      |mod 'toto',
      |   :git => 'ssh://git.backuity.com/home/backuity/git-repos/puppet/puppet-backuity.git'
      |
      |mod 'tata',
      |   :git => "ssh://git.backuity.com/blabla.git"
      |mod 'toto',
      |   :git => 'ssh://ERROR'
      |
    """.stripMargin) must throwAn[Exception].withMessageContaining("Duplicated module toto")
  }

  @Test
  def parseRef() {
    PuppetfileParser.parse(
      """mod "toto",
        |   :git => 'ssh://git.backuity.com/home/backuity/git-repos/puppet/puppet-backuity.git',
        |   :ref => '1.4.2'
        |
        |   mod    'tata',
        |   :git =>    "ssh://git.backuity.com/blabla.git"
      """.stripMargin) must_== Map(
      "toto" -> "ssh://git.backuity.com/home/backuity/git-repos/puppet/puppet-backuity.git",
      "tata" -> "ssh://git.backuity.com/blabla.git"
    )
  }

  @Test
  def parseShouldBeBlanksInsensitive() {
    PuppetfileParser.parse(
     """
       |mod "toto",
       |   :git =>
       |          'ssh://git.backuity.com/home/backuity/git-repos/puppet/puppet-backuity.git'
       |
       |   mod    'ta1_ta',
       |   :git =>    "ssh://git.backuity.com/blabla.git"
     """.stripMargin) must_== Map(
        "toto" -> "ssh://git.backuity.com/home/backuity/git-repos/puppet/puppet-backuity.git",
        "ta1_ta" -> "ssh://git.backuity.com/blabla.git"
    )
  }
}
