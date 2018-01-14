package net.mikolak.travesty
import akka.stream.Shape
import gremlin.scala.ScalaGraph
import guru.nidi.graphviz.model.{Label, Factory => vG, Graph => VizGraph, Node => VizNode}
import gremlin.scala._
import guru.nidi.graphviz.attribute.{MutableAttributed, Rank}
import net.mikolak.travesty.properties.graph.GraphLabelKey

class VizGraphProcessor {

  import VizGraphProcessor._

  def toVizGraph(in: ScalaGraph): VizGraph = {
    var out = vG.graph().directed()

    def rankedSingleton(node: VizNode, rank: Rank) = vG.graph().`with`(node).graphAttr().`with`(rank)

    def implOf(v: Vertex) = v.asScala.property(properties.node.StageImplementation).value()

    val implNames = in.V().l().foldRight(Map.empty[AkkaStage, String]) { (v, map) =>
      val baseName = v.asScala.property(properties.node.StageName).value()
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

    //add graph title
    out = out.generalAttr().`with`(Label.of(in.props(GraphLabelKey).getOrElse("")))

    out
  }

}

object VizGraphProcessor {

  type AkkaStage = akka.stream.Graph[_ <: Shape, _]

  private[travesty] implicit class GraphWithProperties(scalaGraph: ScalaGraph) {
    import scala.compat.java8.OptionConverters._
    val props: (String) => Option[String] = scalaGraph.variables().get(_).asScala
  }

  private[travesty] implicit class AttrScala[T](attrSource: MutableAttributed[T]) {
    import scala.collection.JavaConverters._
    def toMap: Map[String, String] = attrSource.iterator().asScala.map(e => (e.getKey, e.getValue.toString)).toMap
  }

}
