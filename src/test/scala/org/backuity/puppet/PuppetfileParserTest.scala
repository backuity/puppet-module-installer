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
      """.stripMargin) must_== Puppetfile(None, Map(
      "toto" -> Module("ssh://git.backuity.com/home/backuity/git-repos/puppet/puppet-backuity.git", Some("1.4.2")),
      "tata" -> Module("ssh://git.backuity.com/blabla.git")
    ))
  }

  @Test
  def parseForge(): Unit = {
    PuppetfileParser.parse(
      """forge 'http://forge.puppetlabs.com'
        |
        |mod "toto",
        |   :git => 'ssh://git.backuity.com/home/backuity/git-repos/puppet/puppet-backuity.git',
        |   :ref => '1.4.2'
      """.stripMargin) must_== Puppetfile(Some("http://forge.puppetlabs.com"), Map(
      "toto" -> Module("ssh://git.backuity.com/home/backuity/git-repos/puppet/puppet-backuity.git", Some("1.4.2"))
    ))
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
     """.stripMargin) must_== Puppetfile(None, Map(
        "toto" -> Module("ssh://git.backuity.com/home/backuity/git-repos/puppet/puppet-backuity.git"),
        "ta1_ta" -> Module("ssh://git.backuity.com/blabla.git")
    ))
  }
}
