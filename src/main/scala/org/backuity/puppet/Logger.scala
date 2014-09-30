package org.backuity.puppet

trait Logger {

  def warn(msg: String)
}

class AnsiConsoleLogger extends Logger {
  import AnsiFormatter._

  def warn(msg: String): Unit = {
    println(ansi"\yellow{$msg}")
  }
}