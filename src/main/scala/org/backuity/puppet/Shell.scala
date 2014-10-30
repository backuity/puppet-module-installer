package org.backuity.puppet

import java.io.File
import java.nio.file.Path

import org.apache.commons.io.IOUtils

trait Shell {
  /** @throws CommandException if the command failed */
  def exec(cmd: String, path: Path) : String = {
    exec(cmd, path.toFile)
  }

  /** @throws CommandException if the command failed */
  def exec(cmd: String, dir: File) : String
}

case class CommandException(cmd: String, dir: File, returnCode: Int, stderr: String) extends
  RuntimeException(s"Command '$cmd' executed in $dir failed ($returnCode):\n$stderr")

class ShellImpl(implicit log: Logger) extends Shell {
  override def exec(cmd: String, dir: File): String = {
    log.debug(s"$dir $$ $cmd")
    val process = Runtime.getRuntime.exec(ShellImpl.tokenize(cmd), null, dir)
    process.waitFor() match {
      case 0 =>
        val stdout = process.getInputStream
        if( stdout == null ) {
          ""
        } else {
          IOUtils.toString(stdout)
        }

      case errorCode =>
        val stderr = IOUtils.toString(process.getErrorStream)
        throw CommandException(cmd, dir, errorCode, stderr)
    }
  }
}

object ShellImpl {
  def tokenize(cmd: String) : Array[String] = {
    var tokens = List.empty[String]
    var token = ""

    def addToken(): Unit = {
      tokens ::= token
      token = ""
    }

    def addToToken(char: Char): Unit = {
      token += char
    }

    var withinSingleQuote = false
    var withinDoubleQuote = false
    for( i <- 0 until cmd.length) {
      cmd(i) match {
        case '\'' =>
          withinSingleQuote = !withinSingleQuote
          if( withinDoubleQuote ) {
            addToToken('\'')
          }

        case '"' =>
          withinDoubleQuote = !withinDoubleQuote
          if( withinSingleQuote ) {
            addToToken('"')
          }

        case ' ' =>
          if( !withinSingleQuote && !withinDoubleQuote ) {
            addToken()
          } else {
            addToToken(' ')
          }

        case other => addToToken(other)
      }
    }
    if( !token.isEmpty ) addToken()

    tokens.reverse.toArray
  }
}