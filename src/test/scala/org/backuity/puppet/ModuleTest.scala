package org.backuity.puppet

import org.backuity.ansi.AnsiFormatter
import org.backuity.matchete.JunitMatchers
import org.junit.Test

class ModuleTest extends JunitMatchers {

  @Test
  def showGraph(): Unit = {
    val javaModule = Module("java", Some("java-2.7"), "ssh://the.git/java.git", Set(
      Module("base", None, "ssh://base")))

    val graph = Set(
      Module("nexus", Some("v1.2"), "ssh://the.git/nexus", Set(
        javaModule,
        Module("my-sql", None, "ssh://mysql"))),
      Module("jenkins", Some("jenkins-1.2"), "ssh://another.git/jenkins", Set(javaModule)))

    import AnsiFormatter.FormattedHelper

    Module.showGraph(graph, withUri = true) must_==
      ansi"""Puppetfile
        |  ├ jenkins(%bold{1.2.0})%blue{ @ ssh://another.git/jenkins}
        |  │ └ java(%bold{2.7.0})%blue{ @ ssh://the.git/java.git}
        |  │   └ base(%bold{HEAD})%blue{ @ ssh://base}
        |  │
        |  └ nexus(%bold{1.2.0})%blue{ @ ssh://the.git/nexus}
        |    ├ java(%bold{2.7.0})%blue{ @ ssh://the.git/java.git}
        |    └ my-sql(%bold{HEAD})%blue{ @ ssh://mysql}""".stripMargin

    Module.showGraph(graph, withUri = false) must_==
        ansi"""Puppetfile
          |  ├ jenkins(%bold{1.2.0})
          |  │ └ java(%bold{2.7.0})
          |  │   └ base(%bold{HEAD})
          |  │
          |  └ nexus(%bold{1.2.0})
          |    ├ java(%bold{2.7.0})
          |    └ my-sql(%bold{HEAD})""".stripMargin

  }
}