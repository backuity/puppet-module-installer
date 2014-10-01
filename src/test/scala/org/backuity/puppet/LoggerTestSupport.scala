package org.backuity.puppet

trait LoggerTestSupport {

  implicit val logger = new Logger.Console(LogLevel.Debug)
}
