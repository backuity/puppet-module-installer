package org.backuity.puppet

import org.backuity.matchete.{MatcherComparator, Formatter, JunitMatchers}
import org.backuity.puppet.Module.Graph
import org.junit.ComparisonFailure

trait ModuleTestSupport extends JunitMatchers {

  implicit val moduleGraphFormatter = new Formatter[Module.Graph] {
    override def format(graph: Graph): String = Module.showGraph(graph)
  }

  implicit val moduleComparator = new MatcherComparator[Module.Graph] {
    override def checkEqual(actual: Graph, expected: Graph): Unit = {
      if( actual != expected ) throw new ComparisonFailure("Graph are different", Module.showGraph(expected), Module.showGraph(actual))
    }
  }
}
