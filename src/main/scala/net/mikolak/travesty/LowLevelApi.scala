package net.mikolak.travesty
import gremlin.scala.ScalaGraph
import guru.nidi.graphviz.model.{Graph => VizGraph}
import guru.nidi.graphviz.model.{Factory => vG}
import gremlin.scala._

object LowLevelApi {

  def toVizGraph(in: ScalaGraph): VizGraph = {
    var out = vG.graph("stream").directed()

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

    val nodeName = (implOf _).andThen(implNames.apply)

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
