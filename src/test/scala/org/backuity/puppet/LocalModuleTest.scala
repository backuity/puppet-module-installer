package org.backuity.puppet

import org.backuity.matchete.JunitMatchers
import org.junit.Test

class LocalModuleTest extends JunitMatchers {

  @Test
  def version(): Unit ={

    def module(ref: Git.Ref) : LocalModule = LocalModule("toto", ref, isDirty = false, remote = None)

    module(Git.Branch("master")).version must_== Some(Version.Latest)
    module(Git.Tag("v1.1")).version must_== Some(Version("1.1"))

    module(Git.Branch("dev")).version must_== None
    module(Git.Commit("123456789")).version must_== None
  }
}
