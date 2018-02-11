package net.mikolak.travesty

import java.awt.image.BufferedImage
import java.io.File

import akka.stream.StreamDeconstructorProxy
import gremlin.scala._
import guru.nidi.graphviz.engine.Graphviz
import net.mikolak.travesty.render.TypeNameSimplifier
import net.mikolak.travesty.setup.GraphvizEngineType

import org.log4s._

import scala.reflect.runtime.universe._

private[travesty] class Api(deconstruct: StreamDeconstructorProxy,
                            typeNameSimplifier: TypeNameSimplifier,
                            lowLevelApi: VizGraphProcessor,
                            engines: List[GraphvizEngineType.Value]) {

  private val logger = getLogger

  def toImage[T <: AkkaStream: TypeTag](akkaGraph: T, format: ImageFormat, direction: FlowDirection): BufferedImage =
    prepare(akkaGraph, direction).render(format.asJava).toImage

  //noinspection AccessorLikeMethodIsUnit
  def toFile[T <: AkkaStream: TypeTag](akkaGraph: T, format: ImageFormat, direction: FlowDirection)(fileName: String): Unit =
    prepare(akkaGraph, direction).render(format.asJava).toFile(new File(fileName))

  def toString[T <: AkkaStream: TypeTag](akkaGraph: T, format: TextFormat, direction: FlowDirection): String = {
    val graphviz = prepare(akkaGraph, direction)
    if (format == TextFormat.Text) {
      logger.warn(s"Text format support is limited and may not render everything correctly.")
    }

    format.post(graphviz.render(format.asJava))
  }

  def toAbstractGraph[T <: AkkaStream: TypeTag](akkaGraph: T): ScalaGraph = {
    val traversed     = deconstruct(akkaGraph)
    val graphTypeName = typeNameSimplifier(typeOf[T].toString)
    traversed.variables().set(properties.graph.GraphLabelKey, graphTypeName)

    traversed
  }

  private def prepare[T <: AkkaStream: TypeTag](akkaGraph: T, direction: FlowDirection): Graphviz = {
    val in  = toAbstractGraph(akkaGraph)
    val out = lowLevelApi.toVizGraph(in)
    Graphviz.fromGraph(out.generalAttr().`with`(direction.asJava).generalAttr().`with`("labelloc", "top"))
  }

  { //initialize GraphViz engine
    import GraphvizEngineType._
    val first :: rest = engines.map(_.instantiate())
    Graphviz.useEngine(first, rest: _*)
  }
}
