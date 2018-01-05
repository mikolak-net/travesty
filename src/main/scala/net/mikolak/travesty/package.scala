package net.mikolak

import java.awt.image.BufferedImage
import java.io.File

import akka.stream.{Graph, Shape, StreamDeconstructorProxy}
import gremlin.scala._
import guru.nidi.graphviz.engine.{Graphviz, GraphvizJdkEngine, GraphvizV8Engine}
import org.log4s._

package object travesty {

  type AkkaStream = Graph[_ <: Shape, _]

  private val logger = getLogger

  def toImage(akkaGraph: AkkaStream, format: ImageFormat, direction: FlowDirection = LeftToRight): BufferedImage =
    prepare(akkaGraph, direction).render(format.asJava).toImage

  def toFile(akkaGraph: AkkaStream, format: ImageFormat, direction: FlowDirection = LeftToRight)(fileName: String): Unit =
    prepare(akkaGraph, direction).render(format.asJava).toFile(new File(fileName))

  def toString(akkaGraph: AkkaStream, format: TextFormat = Text, direction: FlowDirection = LeftToRight): String = {
    val graphviz = prepare(akkaGraph, direction)
    if (format == Text) {
      logger.warn(s"Text format support is limited and may not render everything correctly.")
    }

    format.post(graphviz.render(format.asJava))
  }

  def toAbstractGraph(akkaGraph: AkkaStream): ScalaGraph = StreamDeconstructorProxy(akkaGraph)

  private def prepare(akkaGraph: AkkaStream, direction: FlowDirection): Graphviz = {
    val in  = toAbstractGraph(akkaGraph)
    val out = LowLevelApi.toVizGraph(in)
    Graphviz.fromGraph(out.generalAttr().`with`(direction.asJava))
  }

  { //initialize GraphViz engine
    Graphviz.useEngine(new GraphvizV8Engine, new GraphvizJdkEngine)
  }
}
