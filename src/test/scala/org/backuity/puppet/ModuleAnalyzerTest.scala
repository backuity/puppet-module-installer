package org.backuity.puppet

import java.nio.file.Files

import org.junit.Test

class ModuleAnalyzerTest extends GitTestSupport {

  @Test
  def analyzeMissingFolder(): Unit = {
    val modules = Files.createTempDirectory("pmi").resolve("missing")
    Files.isDirectory(modules) must beFalse
    new ModuleAnalyzer(git).analyze(modules) must beEmpty
  }

  // TODO remote origin
  @Test
  def analyze(): Unit = {
    val modules = Files.createTempDirectory("pmi")
    modules.createGitRepo("nexus").addFileToGit("x").tagAndCheckout("v1.2")
    modules.createGitRepo("jenkins").addFileToGit("x").tagAndCheckout("v2.9")
    modules.createGitRepo("java").addFileToGit("x").path.addFiles("uncommited" -> "stuff")

    new ModuleAnalyzer(git).analyze(modules) must_== Set(
      LocalModule("nexus", Git.Tag("v1.2"), isDirty = false),
      LocalModule("jenkins", Git.Tag("v2.9"), isDirty = false),
      LocalModule("java", Git.Branch("master"), isDirty = true)
    )
  }
}
