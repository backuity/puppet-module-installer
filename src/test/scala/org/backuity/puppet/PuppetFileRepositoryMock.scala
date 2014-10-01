package org.backuity.puppet

import java.nio.file.Path

import org.backuity.matchete.JunitMatchers
import org.backuity.puppet.PuppetFileRepository.{Missing, NotFound, Found, Entry}
import org.backuity.puppet.Version.MajorMinorBugFix

import scala.collection.concurrent.TrieMap

/**
 * A dummy repo that makes sure we don't add things twice
 */
class PuppetFileRepositoryMock extends PuppetFileRepository with JunitMatchers {
  val addFlags = TrieMap.empty[(String,Version), Path]
  val missing = TrieMap.empty[(String,Version), Boolean]

  override def add(name: String, version: MajorMinorBugFix, file: Path): Path = {
    addFlags.putIfAbsent((name, version), file) must beEmpty
    file
  }

  override def find(name: String, version: MajorMinorBugFix): Entry = {
    missing.get((name,version)) match {
      case Some(_) => Missing
      case None =>
        addFlags.get((name, version)) match {
          case None => NotFound
          case Some(path) => Found(path)
        }
    }
  }

  override def addMissing(name: String, version: MajorMinorBugFix): Unit = {
    missing.putIfAbsent((name, version), true) must beEmpty
  }
}
