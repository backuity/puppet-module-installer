package org.backuity.puppet

import java.net.URL
import java.util.jar.Manifest

object VersionUtil {

  private def toManifest(url: URL) : Manifest = {
    new Manifest(url.openStream())
  }

  def versionFor(projectName : String) : Option[String] = {
    import scala.collection.JavaConversions._
    val manifests: List[URL] = this.getClass.getClassLoader.getResources("META-INF/MANIFEST.MF").toList
    manifests.map(toManifest).find( _.getMainAttributes.getValue("Implementation-Title") == projectName)
        .map( _.getMainAttributes.getValue("Implementation-Version"))
  }
}
