package org.backuity.puppet

import java.nio.file.Files

import org.backuity.matchete.JunitMatchers
import org.backuity.puppet.PuppetFileRepository.Found
import org.junit.Test

class PuppetFileRepositoryImplTest extends JunitMatchers with FilesTestUtil with LoggerTestSupport {

  @Test
  def createBaseDirsIfAbsent(): Unit = {
    val tmpDir = Files.createTempDirectory("pmi")
    val baseDir = tmpDir.resolve("puppet-module-installer").resolve("puppet-file")
    val repo = new PuppetFileRepositoryImpl(baseDir)

    val tmpFile = Files.createTempFile(tmpDir, "stuff", "")
    tmpFile.write("blabla")
    val savedFile = repo.add("nexus", Version("1.2"), tmpFile)
    savedFile.toString must not(beEqualTo(tmpFile.toString))
    savedFile must contain("blabla")
  }

  @Test
  def find(): Unit = {
    val tmpDir = Files.createTempDirectory("pmi")
    val baseDir = tmpDir.resolve("puppet-module-installer")
    val repo = new PuppetFileRepositoryImpl(baseDir)

    val tmpFile = Files.createTempFile(tmpDir, "stuff", "")
    tmpFile.write("blabla")
    val savedFile = repo.add("nexus", Version("1.2"), tmpFile)
    savedFile.toString must not(beEqualTo(tmpFile.toString))
    savedFile must contain("blabla")

    repo.find("nexus", Version("1.2")) must be("a found class") {
      case Found(path) => path.toString must_== savedFile.toString
    }
  }
}
