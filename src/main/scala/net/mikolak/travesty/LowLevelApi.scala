package net.mikolak.travesty
import gremlin.scala.ScalaGraph
import guru.nidi.graphviz.model.{Node => VizNode, Factory => vG, Graph => VizGraph}
import gremlin.scala._
import guru.nidi.graphviz.attribute.Rank

object LowLevelApi {

  def toVizGraph(in: ScalaGraph): VizGraph = {
    var out = vG.graph("stream").directed()

    def rankedSingleton(node: VizNode, rank: Rank) = vG.graph().`with`(node).graphAttr().`with`(rank)

    def implOf(v: Vertex) = v.asScala.property(properties.StageImplementation).value()

    val implNames = in.V().l().foldRight(Map.empty[AkkaStream, String]) { (v, map) =>
      val baseName = v.asScala.property(properties.StageName).value()
      val impl     = implOf(v)
      if (map.contains(impl)) {
        map
      } else {
        val possibleNames = Stream.iterate(0)(_ + 1).map(i => baseName + (if (i == 0) "" else s"($i)"))
        val mapVals       = map.values.toSet
        possibleNames.find(!mapVals.contains(_)).map(name => map + ((impl, name))).getOrElse(map)
      }
    }

    val nodeFor = (implOf _).andThen(implNames.apply).andThen(vG.node)

    for { v <- in.V().l() } {
      val baseNode = nodeFor(v)

      val possiblyRankedNode = if (v.outE().notExists()) {
        rankedSingleton(baseNode, Rank.SINK)
      } else if (v.inE().notExists()) {
        rankedSingleton(baseNode, Rank.SOURCE)
      } else {
        baseNode
      }

      out = out.`with`(possiblyRankedNode)
    }

    //second pass is necessary since otherwise graphviz-java would immediately linked nodes
    // (i.e. ones directly after sources/before sinks) within the rank subgraphs, screwing up
    // the painting order. The "duplication" prevents this from occuring.
    for {
      v <- in.V().l()
    } {
      var node = nodeFor(v)

      for { e <- v.outE().l() } {
        node = node.link(nodeFor(e.inVertex()))
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
