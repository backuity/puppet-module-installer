package org.backuity.puppet

import java.io.{StringWriter, PrintWriter}

import org.backuity.puppet.LogLevel.LogLevel

trait Logger {
  def debug(s: String)
  def debug(t: Throwable)
  def info(msg: String)
  def warn(msg: String)
  def error(msg: String)
}

object LogLevel extends Enumeration {
  type LogLevel = Value
  val Error = Value(0)
  val Warn = Value(1)
  val Info = Value(2)
  val Debug = Value(3)
}

object Logger {
  class AnsiConsole(logLevel: LogLevel) extends Console(logLevel) {
    import AnsiFormatter._

    override def error(msg: String): Unit = {
      super.error(red(msg))
    }

    override def warn(msg: String): Unit = {
      super.warn(yellow(msg))
    }

    override def debug(msg: String): Unit = {
      super.debug(blue(msg))
    }

    override def debug(t: Throwable): Unit = {
      val sw = new StringWriter()
      val pw = new PrintWriter(sw)
      t.printStackTrace(pw)
      super.debug(blue(sw.toString))
    }
  }

  class Console(logLevel : LogLevel) extends Logger {

    override def debug(msg: String): Unit ={
      if( logLevel >= LogLevel.Debug ) {
        println(msg)
      }
    }

    override def debug(t: Throwable): Unit = {
      if( logLevel >= LogLevel.Debug ) {
        t.printStackTrace()
      }
    }

    override def info(msg: String): Unit = {
      if( logLevel >= LogLevel.Info ) {
        println(msg)
      }
    }

    override def warn(msg: String): Unit = {
      if( logLevel >= LogLevel.Warn ) {
        println(msg)
      }
    }

    override def error(msg: String): Unit = {
      println(msg)
    }
  }
}

