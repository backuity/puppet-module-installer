package org.backuity.puppet

case class LocalModule(name: String, ref: Git.Ref, remote : Option[String] = None) {

  lazy val version : Option[Version] = ref match {
    case Git.Branch("master") => Some(Version.Latest)
    case Git.Tag(tag) => Some(Version(tag))
    case _ => None
  }
}
