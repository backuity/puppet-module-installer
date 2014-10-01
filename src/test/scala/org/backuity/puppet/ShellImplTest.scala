package org.backuity.puppet

import org.backuity.matchete.JunitMatchers
import org.junit.Test

class ShellImplTest extends JunitMatchers {

  @Test
  def tokenizeShouldNotSplitQuotedText(): Unit = {
    ShellImpl.tokenize("this is a 'quoted text'") must_== Array("this", "is", "a", "quoted text")
    ShellImpl.tokenize("a \"quoted text\" kept") must_== Array("a", "quoted text", "kept")
    ShellImpl.tokenize("a \"quoted 'yo' text\" kept") must_== Array("a", "quoted 'yo' text", "kept")
    ShellImpl.tokenize("a 'quoted \"yo\" text' kept") must_== Array("a", "quoted \"yo\" text", "kept")
  }
}
