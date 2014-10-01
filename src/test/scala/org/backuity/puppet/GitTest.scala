package org.backuity.puppet

import java.io.FileNotFoundException
import java.nio.file.{Path, Files, Paths}

import org.backuity.matchete.JunitMatchers
import org.junit.Test

class GitTest extends JunitMatchers with LoggerTestSupport with FilesTestUtil {

  @Test
  def latestVersion_vPrefix(): Unit = {
    val gitOutput = """3616c5b452d63bf45ee3485f6c4632175960a5e6	refs/tags/v1.0
                      |15d871d379230e7074fd21eae10af719ced6368d	refs/tags/v1.1
                      |3ba8891c91a41258400e723d1d67a1c4e2c6d338	refs/tags/v1.1^{}
                      |ab716f4e206bd410074cb49a8308246206a64ab0	refs/tags/v1.10
                      |d2abc5baa22caaee08a3d7ee8fcd089566247532	refs/tags/v1.10^{}
                      |e725cfb257ec50e268532bd3ab2e3d9623b1c2eb	refs/tags/v1.2
                      |1482ee0dc1e5886a77c6dd80da56464a6507135e	refs/tags/v1.2^{}
                      |9a4cf4e2103400c1f7ad82d182586343b7231c1f	refs/tags/v1.9
                      |b2448b54b1d398febddaf71919234785f1afaf43	refs/tags/v1.9^{}
                      |"""

    Git.latestVersion(gitOutput) must_== Some("v1.10")
  }

  @Test
  def latestVersion_namePrefix(): Unit = {
    val gitOutput = """8f6996ea3f40c9e35f097d902c79bb806b60efe9	refs/tags/puppet-dynatrace-1.0.0
                      |cce15b848f46200efb3ea0bc235b3d602bd995a2	refs/tags/puppet-dynatrace-1.0.0^{}
                      |0a8edba0afaf2edaa64abffcd5e46951af3602bd	refs/tags/puppet-dynatrace-1.1
                      |4b65eea7cc06d9db3ce312f93e7bcf793f242dc0	refs/tags/puppet-dynatrace-1.1^{}
                      |"""
    Git.latestVersion(gitOutput) must_== Some("puppet-dynatrace-1.1")
  }

  @Test
  def latestVersion_ignoreIncorrectVersion(): Unit = {
    val gitOutput = """8f6996ea3f40c9e35f097d902c79bb806b60efe9	refs/tags/puppet-dynatrace-1.0.0
                      |cce15b848f46200efb3ea0bc235b3d602bd995a2	refs/tags/puppet-dynatrace-1.0.0^{}
                      |0a8edba0afaf2edaa64abffcd5e46951af3602bd	refs/tags/puppet-dynatrace-1.1-RELEASE
                      |4b65eea7cc06d9db3ce312f93e7bcf793f242dc0	refs/tags/puppet-dynatrace-1.1^{}
                      |"""
    Git.latestVersion(gitOutput) must_== Some("puppet-dynatrace-1.0.0")
  }


  @Test
  def latestVersion_noVersion(): Unit = {
    Git.latestVersion("") must_== None
  }

  val shell = new ShellImpl()
  val git = new Git.Impl(shell)

  private def addToGit(fileName: String, sourceDir: Path): Unit = {
    sourceDir.addFiles(fileName -> (fileName + " content"))
    shell.exec("git add -A", sourceDir)
    shell.exec(s"git commit -m 'added $fileName'", sourceDir)
  }

  @Test
  def updateFromBranchToMaster(): Unit = {
    val baseDir = Files.createTempDirectory("git")

    val sourceDir = baseDir.resolve("source")
    Files.createDirectory(sourceDir)

    val destDir = baseDir.resolve("dest")
    Files.createDirectory(destDir)

    shell.exec("git init", sourceDir)
    addToGit("toto_1", sourceDir)
    shell.exec("git checkout -b abranch", sourceDir)
    addToGit("toto_2", sourceDir)
    shell.exec("git checkout master", sourceDir)
    addToGit("toto_3", sourceDir)

    shell.exec("git clone --branch abranch ../source .", destDir)
    destDir / "toto_3" must not(exist)

    git.update("../source", None, destDir)
    destDir / "toto_3" must contain("toto_3 content")
  }

  @Test
  def currentBranch(): Unit = {
    val dir = Files.createTempDirectory("git")

    shell.exec("git init", dir)
    addToGit("toto_1", dir)
    git.currentBranch(dir) must_== "master"

    shell.exec("git checkout -b a_branch", dir)
    addToGit("toto_2", dir)
    git.currentBranch(dir) must_== "a_branch"
  }

  @Test
  def updateFromBranchToBranch(): Unit = {
    val baseDir = Files.createTempDirectory("git")

    val sourceDir = baseDir.resolve("source")
    Files.createDirectory(sourceDir)

    val destDir = baseDir.resolve("dest")
    Files.createDirectory(destDir)

    shell.exec("git init", sourceDir)
    addToGit("toto_1", sourceDir)
    shell.exec("git checkout -b a_branch", sourceDir)
    addToGit("toto_2", sourceDir)

    shell.exec("git clone --branch a_branch ../source .", destDir)

    shell.exec("git checkout -b b_branch", sourceDir)
    addToGit("toto_3", sourceDir)

    git.update("../source", Some("b_branch"), destDir)
    destDir / "toto_3" must contain("toto_3 content")
  }

  @Test
  def downloadFile(): Unit = {
    val dir = Files.createTempDirectory("git")

    shell.exec("git init", dir)
    addToGit("toto_1", dir)

    git.downloadFile("toto_1", "file://" + dir, None) must contain("toto_1 content")
  }

  @Test
  def downloadFileOfASpecificBranch(): Unit = {
    val dir = Files.createTempDirectory("git")

    shell.exec("git init", dir)
    addToGit("toto_1", dir)
    shell.exec("git checkout -b a_branch", dir)
    addToGit("toto_2", dir)
    shell.exec("git checkout master", dir)

    git.downloadFile("toto_2", "file://" + dir, None) must throwA[FileNotFoundException]
    git.downloadFile("toto_2", "file://" + dir, Some("a_branch")) must contain("toto_2 content")
  }

  @Test
  def downloadUnexistingFileShouldThrowFileNotFoundException(): Unit = {
    val dir = Files.createTempDirectory("git")

    shell.exec("git init", dir)
    addToGit("toto_1", dir)

    git.downloadFile("unexisting", "file://" + dir, None) must throwA[FileNotFoundException]
  }
}
