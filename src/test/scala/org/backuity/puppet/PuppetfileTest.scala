package org.backuity.puppet

import org.backuity.matchete.JunitMatchers
import org.backuity.puppet.Puppetfile.{ForgeModule, GitModule}
import org.junit.Test

class PuppetfileTest extends JunitMatchers {

  @Test
  def duplicatedModuleDefinitionsIsntAllowed() {
    Puppetfile.parse("""
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
  def commentsShouldBeIgnored(): Unit = {
    Puppetfile.parse(
      """# a comment
        |mod "toto", # tada
        |   # a ... comment! -- everything on the line should be ignore :git => 'ssh://git.backuity.com/',
        |   :git => 'ssh://git.backuity.com/home/backuity/git-repos/puppet/puppet-backuity.git', # yipee
        |   # another
        |   # multiline
        |   # comment
        |   :ref => '1.4.2' # ooooh
        |
        |# yet another
        |# multi
        |# line comment
        |mod 'tata',
        |   :git => # bloublou
        |     "ssh://git.backuity.com/blabla.git"
        |""".stripMargin) must_== Puppetfile(None, Map(
      "toto" -> GitModule("ssh://git.backuity.com/home/backuity/git-repos/puppet/puppet-backuity.git", Some("1.4.2")),
      "tata" -> GitModule("ssh://git.backuity.com/blabla.git")
    ))
  }

  @Test
  def parseRef() {
    Puppetfile.parse(
      """mod "toto",
        |   :git => 'ssh://git.backuity.com/home/backuity/git-repos/puppet/puppet-backuity.git',
        |   :ref => '1.4.2'
        |
        |   mod    'tata',
        |   :git =>    "ssh://git.backuity.com/blabla.git"
      """.stripMargin) must_== Puppetfile(None, Map(
      "toto" -> GitModule("ssh://git.backuity.com/home/backuity/git-repos/puppet/puppet-backuity.git", Some("1.4.2")),
      "tata" -> GitModule("ssh://git.backuity.com/blabla.git")
    ))
  }

  @Test
  def parseForgeModule() {
    Puppetfile.parse(
      """forge 'http://forge.puppetlabs.com'
        |
        |mod 'puppetlabs/stdlib', '>=0.1.6'
        |mod 'puppetlabs/apt', '>=1.0.0'
        |mod 'puppetlabs/concat', '>=1.0.0'
        |""".stripMargin) must_== Puppetfile(Some("http://forge.puppetlabs.com"), Map(
      "puppetlabs/stdlib" -> ForgeModule(">=0.1.6"),
      "puppetlabs/apt" -> ForgeModule(">=1.0.0"),
      "puppetlabs/concat" -> ForgeModule(">=1.0.0")
    ))
  }

  @Test
  def parseForge(): Unit = {
    Puppetfile.parse(
      """forge 'http://forge.puppetlabs.com'
        |
        |mod "toto",
        |   :git => 'ssh://git.backuity.com/home/backuity/git-repos/puppet/puppet-backuity.git',
        |   :ref => '1.4.2'
      """.stripMargin) must_== Puppetfile(Some("http://forge.puppetlabs.com"), Map(
      "toto" -> GitModule("ssh://git.backuity.com/home/backuity/git-repos/puppet/puppet-backuity.git", Some("1.4.2"))
    ))
  }

  @Test
  def parseShouldBeBlanksInsensitive() {
    Puppetfile.parse(
     """
       |mod "toto",
       |   :git =>
       |          'ssh://git.backuity.com/home/backuity/git-repos/puppet/puppet-backuity.git'
       |
       |   mod    'ta1_ta',
       |   :git =>    "ssh://git.backuity.com/blabla.git"
     """.stripMargin) must_== Puppetfile(None, Map(
        "toto" -> GitModule("ssh://git.backuity.com/home/backuity/git-repos/puppet/puppet-backuity.git"),
        "ta1_ta" -> GitModule("ssh://git.backuity.com/blabla.git")
    ))
  }
}
