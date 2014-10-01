package org.backuity.puppet

import java.util.concurrent.ExecutorService

object ConcurrentUtil {

  implicit class PimpExecutorService(val ec: ExecutorService) extends AnyVal {
    def run(f : => Unit): Unit = {
      ec.execute(new Runnable {
        override def run(): Unit = f
      })
    }
  }
}
