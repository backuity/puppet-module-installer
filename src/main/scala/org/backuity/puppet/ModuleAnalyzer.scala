package org.backuity.puppet

import java.io.File
import java.nio.file.Path

class ModuleAnalyzer(git : Git)(implicit log: Logger) {

  def analyze(modulePath: Path) : Set[LocalModule] = {
    val modules : Set[File] = modulePath.toFile.listFiles() match {
      case null => Set.empty
      case array => array.toSet
    }
    for (module <- modules if module.isDirectory && git.isGit(module.toPath)) yield {
      LocalModule(module.getName, git.currentRef(module.toPath), git.isDirty(module.toPath), None)
    }
  }
}
