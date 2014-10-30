package org.backuity.puppet

import java.nio.file.{Files, Path}

import org.backuity.matchete.JunitMatchers

trait GitTestSupport extends JunitMatchers with FilesTestUtil with LoggerTestSupport {
  
  val shell = new ShellImpl()
  val git = new Git.Impl(shell)

  implicit def toGitRepoFactory(parent: Path) = new GitRepoFactory(parent, git, shell)
  implicit def toGitRepo(path: Path) = new GitRepo(path, git, shell)
}

class GitRepoFactory(parent: Path, git: Git, shell: Shell) {
  def createGitRepo(name: String) : GitRepo = {
    val path = parent.resolve(name)
    Files.createDirectory(path)
    val repo = new GitRepo(path, git, shell)
    repo.initGit()
  }
}

class GitRepo(val path: Path, git: Git, shell: Shell) {
  
  def initGit(): GitRepo = {
    shell.exec("git init", path)
    this
  }

  def addFileToGit(fileName: String): GitRepo = {
    new PimpPath(path).addFiles(fileName -> (fileName + " content"))
    shell.exec("git add -A", path)
    shell.exec(s"git commit -m 'added $fileName'", path)
    this
  }
  
  def tag(name: String): GitRepo = {
    shell.exec(s"git tag $name", path)
    this
  }

  def checkout(ref: String) : GitRepo = {
    shell.exec(s"git checkout $ref", path)
    this
  }

  def tagAndCheckout(name: String) : GitRepo = {
    tag(name).checkout(name)
  }

  /** create a branch and add a file "file-$name" to make sure the branch isn't ignored */
  def branch(name: String) : GitRepo = {
    shell.exec(s"git checkout -b $name", path)
    addFileToGit("file-" + name)
    this
  }
}