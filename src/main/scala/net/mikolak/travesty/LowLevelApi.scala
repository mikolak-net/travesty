package net.mikolak.travesty
import gremlin.scala.ScalaGraph
import guru.nidi.graphviz.model.{Graph => VizGraph}
import guru.nidi.graphviz.model.{Factory => vG}
import gremlin.scala._

object LowLevelApi {

  def toVizGraph(in: ScalaGraph): VizGraph = {
    var out = vG.graph("stream").directed()

    def nodeName(v: Vertex) = v.asScala.property(properties.StageName).value()

    for { v <- in.V().l() } {
      var node = vG.node(nodeName(v))
      for (e <- v.outE().l()) {
        node = node.link(nodeName(e.inVertex()))
      }
      out = out.`with`(node)
    }
    out
  }

  object properties {
    val StageName: Key[String]               = Key[String]("stageName")
    val ImplementationName: Key[String]      = Key[String]("implName")
    val StageImplementation: Key[AkkaStream] = Key[AkkaStream]("stageImpl")
  }

  val EdgeLabel = "to"

}
