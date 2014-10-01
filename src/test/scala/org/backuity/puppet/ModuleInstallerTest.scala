package org.backuity.puppet

import org.backuity.matchete.JunitMatchers
import org.junit.Test

class ModuleInstallerTest extends JunitMatchers {

  @Test
  def flattenModuleGraphMustFailOnInconsistentVersioning(): Unit = {
    ModuleInstaller.flattenModuleGraph(Set(
      Module("nexus", Some("1.0"), "nexus-uri", Set(
        Module("java", Some("2.1"), "java-uri")
      )),
      Module("jenkins", Some("8.2"), "jenkins-uri", Set(
        Module("java", Some("1.2"), "java-uri")
      ))
    )) must throwAn[IllegalArgumentException].withMessage(
      "Incompatible version of module java: nexus -> java(2.1.0), jenkins -> java(1.2.0)"
    )
  }

  @Test
  def flattenModuleGraphMustSelectHighestCompatibleVersion(): Unit = {
    ModuleInstaller.flattenModuleGraph(Set(
      Module("nexus", Some("1.0"), "nexus-uri", Set(
        Module("java", Some("1.12"), "java-uri")
      )),
      Module("jenkins", Some("8.2"), "jenkins-uri", Set(
        Module("java", Some("1.2"), "java-uri")
      ))
    )) must_== Map(
      "nexus"   -> Module.Description("nexus", Some("1.0"), "nexus-uri"),
      "jenkins" -> Module.Description("jenkins", Some("8.2"), "jenkins-uri"),
      "java"    -> Module.Description("java", Some("1.12"), "java-uri")
    )
  }
}
