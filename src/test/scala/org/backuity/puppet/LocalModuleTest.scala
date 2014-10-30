package org.backuity.puppet

import org.backuity.matchete.JunitMatchers
import org.junit.Test

class LocalModuleTest extends JunitMatchers {

  @Test
  def version(): Unit ={
    LocalModule("toto", Git.Branch("master")).version must_== Some(Version.Latest)
    LocalModule("toto", Git.Tag("v1.1")).version must_== Some(Version("1.1"))

    LocalModule("toto", Git.Branch("dev")).version must_== None
    LocalModule("toto", Git.Commit("123456789")).version must_== None
  }
}
