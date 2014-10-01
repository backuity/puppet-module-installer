package org.backuity.puppet

import java.io.FileNotFoundException
import java.nio.file.Files

import org.backuity.puppet.ModuleFetcher.{FetchMode, CyclicDependencyException}
import org.backuity.puppet.PuppetFileRepository.Found
import org.junit.Test
import org.mockito.Matchers.{anyObject, anyString}
import org.mockito.Mockito.{mock, times, verify, when}

class ModuleFetcherTest extends ModuleTestSupport with LoggerTestSupport with FilesTestUtil {

  @Test
  def fetchModulesRecursively_latestMode(): Unit = {
    val git = mock(classOf[Git])

    val gitFolder = Files.createTempDirectory("pmi")
    gitFolder.addFiles(
      "server" ->
        """mod "nexus",
          |   :git => 'ssh://some.git/nexus'
          |
          |mod "jenkins",
          |   :git => 'ssh://some.git/jenkins',
          |   :ref => 'v1.0'
        """.stripMargin,

      "nexus-9.0.0" ->
        """mod "java",
          |   :git => 'ssh://some.git/java',
          |   :ref => 'v1.2'
        """.stripMargin,

      "jenkins-1.2" ->
        """mod "java",
          |   :git => 'ssh://some.git/java',
          |   :ref => 'v2.5'
        """.stripMargin,

      "java-2.7" ->
        """mod "base",
          |   :git => 'ssh://some.git/base'
        """.stripMargin,

      "base" -> ""
    )

    when(git.latestTag("ssh://some.git/nexus")).thenReturn(Some("nexus-9.0.0"))
    when(git.latestTag("ssh://some.git/jenkins")).thenReturn(Some("v1.2"))
    when(git.latestTag("ssh://some.git/java")).thenReturn(Some("v2.7"))
    when(git.latestTag("ssh://some.git/base")).thenReturn(Some("v2.0"))

    when(git.downloadFile("Puppetfile", "ssh://some.git/nexus", Some("nexus-9.0.0"))).thenReturn(gitFolder.resolve("nexus-9.0.0"))
    when(git.downloadFile("Puppetfile", "ssh://some.git/jenkins", Some("v1.2"))).thenReturn(gitFolder.resolve("jenkins-1.2"))
    when(git.downloadFile("Puppetfile", "ssh://some.git/java", Some("v2.7"))).thenReturn(gitFolder.resolve("java-2.7"))
    when(git.downloadFile("Puppetfile", "ssh://some.git/base", Some("v2.0"))).thenReturn(gitFolder.resolve("base"))

    val puppetFileRepo = new PuppetFileRepositoryMock
    val fetcher = new ModuleFetcher(git, puppetFileRepo)

    val javaModule = Module("java", Some("v2.7"), "ssh://some.git/java", Set(
      Module("base", Some("v2.0"), "ssh://some.git/base")
    ))

    fetcher.buildModuleGraph(gitFolder.resolve("server"), FetchMode.LATEST) must_== Set(
      Module("nexus", Some("nexus-9.0.0"), "ssh://some.git/nexus", Set(javaModule)),
      Module("jenkins", Some("v1.2"), "ssh://some.git/jenkins", Set(javaModule))
    )

    puppetFileRepo.find("java", Version("2.7")) must beA[Found]
    puppetFileRepo.find("base", Version("2.0")) must beA[Found]
    puppetFileRepo.find("jenkins", Version("1.2")) must beA[Found]
    puppetFileRepo.find("nexus", Version("9.0.0")) must beA[Found]

    verify(git, times(4)).latestTag(anyString())(anyObject())
  }

  @Test
  def fetchModulesRecursively_normalMode(): Unit = {
    val git = mock(classOf[Git])

    val gitFolder = Files.createTempDirectory("pmi")
    gitFolder.addFiles(
      "server" ->
          """mod "nexus",
            |   :git => 'ssh://some.git/nexus'
            |
            |mod "jenkins",
            |   :git => 'ssh://some.git/jenkins',
            |   :ref => 'v1.0'
          """.stripMargin,

      "nexus" ->
          """mod "java",
            |   :git => 'ssh://some.git/java',
            |   :ref => 'v1.2'
          """.stripMargin,

      "jenkins-1.0" ->
          """mod "java",
            |   :git => 'ssh://some.git/java',
            |   :ref => 'v2.5'
          """.stripMargin,

      "java-2.5" ->
          """mod "base",
            |   :git => 'ssh://some.git/base'
          """.stripMargin,

      "base" -> ""
    )

    when(git.downloadFile("Puppetfile", "ssh://some.git/nexus", None)).thenReturn(gitFolder.resolve("nexus"))
    when(git.downloadFile("Puppetfile", "ssh://some.git/jenkins", Some("v1.0"))).thenReturn(gitFolder.resolve("jenkins-1.0"))
    when(git.downloadFile("Puppetfile", "ssh://some.git/java", Some("v1.2"))).thenReturn(gitFolder.resolve("java-2.5"))
    when(git.downloadFile("Puppetfile", "ssh://some.git/java", Some("v2.5"))).thenReturn(gitFolder.resolve("java-2.5"))
    when(git.downloadFile("Puppetfile", "ssh://some.git/base", None)).thenReturn(gitFolder.resolve("base"))

    val puppetFileRepo = new PuppetFileRepositoryMock
    val fetcher = new ModuleFetcher(git, puppetFileRepo)

    fetcher.buildModuleGraph(gitFolder.resolve("server"), FetchMode.NORMAL) must_== Set(
      Module("nexus", None, "ssh://some.git/nexus", Set(
        Module("java", Some("v1.2"), "ssh://some.git/java", Set(
          Module("base", None, "ssh://some.git/base")
      )))),
      Module("jenkins", Some("v1.0"), "ssh://some.git/jenkins", Set(
        Module("java", Some("v2.5"), "ssh://some.git/java", Set(
          Module("base", None, "ssh://some.git/base")
      ))))
    )

    verify(git, times(0)).latestTag(anyString())(anyObject())
  }

  @Test
  def detectCyclicDependency(): Unit = {
    val git = mock(classOf[Git])

    val gitFolder = Files.createTempDirectory("pmi")
    gitFolder.addFiles(
      "a" ->
          """mod "b",
            |   :git => 'ssh://some.git/b'
          """.stripMargin,

      "b" ->
          """mod "a",
            |   :git => 'ssh://some.git/a'
          """.stripMargin
    )

    when(git.downloadFile("Puppetfile", "ssh://some.git/a", None)).thenReturn(gitFolder.resolve("a"))
    when(git.downloadFile("Puppetfile", "ssh://some.git/b", None)).thenReturn(gitFolder.resolve("b"))

    val puppetFileRepo = new PuppetFileRepositoryMock
    val fetcher = new ModuleFetcher(git, puppetFileRepo)

    fetcher.buildModuleGraph(gitFolder.resolve("b"), FetchMode.NORMAL) must throwA[CyclicDependencyException].withMessage(
      "Cyclic dependency found : a -> b -> a")
  }

  @Test
  def ignoreDependenciesOfModulesWithoutPuppetfile(): Unit = {
    val git = mock(classOf[Git])

    val gitFolder = Files.createTempDirectory("pmi")
    gitFolder.addFiles(
      "server" ->
          """mod "nexus",
            |   :git => 'ssh://some.git/nexus'
            |
            |mod "jenkins",
            |   :git => 'ssh://some.git/jenkins',
            |   :ref => 'v1.0'
            |
            |mod 'other',
            |   :git => 'ssh://some.git/other'
          """.stripMargin,

      "nexus" -> ""
    )

    when(git.downloadFile("Puppetfile", "ssh://some.git/nexus", None)).thenReturn(gitFolder.resolve("nexus"))
    when(git.downloadFile("Puppetfile", "ssh://some.git/jenkins", Some("v1.0"))).thenThrow(new FileNotFoundException("puppetfile not found"))
    when(git.downloadFile("Puppetfile", "ssh://some.git/other", None)).thenThrow(new FileNotFoundException("puppetfile not found"))

    val puppetFileRepo = new PuppetFileRepositoryMock
    val fetcher = new ModuleFetcher(git, puppetFileRepo)

    fetcher.buildModuleGraph(gitFolder.resolve("server"), FetchMode.NORMAL) must_== Set(
      Module("nexus", None, "ssh://some.git/nexus"),
      Module("jenkins", Some("v1.0"), "ssh://some.git/jenkins"),
      Module("other", None, "ssh://some.git/other")
    )
  }
}
