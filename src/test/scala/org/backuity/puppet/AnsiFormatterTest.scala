package org.backuity.puppet

import org.backuity.matchete.JunitMatchers
import org.junit.Test

import AnsiFormatter._

class AnsiFormatterTest extends JunitMatchers {

  @Test
  def brokenSyntaxShouldNotThrowException(): Unit = {
    ansi"\yellow{MISSING BRACKET" must_== "\\yellow{MISSING BRACKET"
  }
}
