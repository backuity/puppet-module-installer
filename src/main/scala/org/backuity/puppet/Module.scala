package org.backuity.puppet

import AnsiFormatter._

case class Module(description: Module.Description, dependencies: Module.Graph) {
  def name = description.name
  def version = description.version
  def tag = description.tag
  def uri = description.uri
}

object Module {
  type Graph = Set[Module]

  def apply(name: String, tag: Option[String], uri: String, dependencies: Set[Module] = Set.empty) : Module = {
    Module(Module.Description(name,tag,uri),dependencies)
  }

  case class Description(name: String, tag: Option[String], uri: String) {
    lazy val version = Version(tag)
  }

  def showGraph(graph: Graph, withUri: Boolean = true) : String = {
    val root = Module("Puppetfile", None, "", graph)
    def showModule(m: Module) = {
      if( m.name == "Puppetfile" ) {
        "Puppetfile"
      } else {
        val uri = if( withUri ) ansi"\blue{ @ ${m.uri}}" else ""
        ansi"${m.name}(\bold{${m.version}})$uri"
      }
    }
    Graph.toAscii[Module](root, _.dependencies.toList.sortBy(_.name), showModule)
  }
}