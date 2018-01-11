package net.mikolak

import java.awt.image.BufferedImage
import java.io.File

import akka.stream.{ClosedShape, Graph, StreamDeconstructorProxy}
import gremlin.scala._
import guru.nidi.graphviz.engine.{Graphviz, GraphvizCmdLineEngine, GraphvizJdkEngine, GraphvizV8Engine}
import net.mikolak.travesty.LowLevelApi.AkkaStage
import net.mikolak.travesty.render.PackageNameSimplifier
import org.log4s._

import scala.reflect.runtime.universe._

package object travesty {

  type AkkaStream = Graph[_ <: ClosedShape, _]

  private val logger = getLogger

  def toImage[T <: AkkaStream: TypeTag](akkaGraph: T,
                                        format: ImageFormat,
                                        direction: FlowDirection = LeftToRight): BufferedImage =
    prepare(akkaGraph, direction).render(format.asJava).toImage

  //noinspection AccessorLikeMethodIsUnit
  def toFile[T <: AkkaStream: TypeTag](akkaGraph: T, format: ImageFormat, direction: FlowDirection = LeftToRight)(
      fileName: String): Unit =
    prepare(akkaGraph, direction).render(format.asJava).toFile(new File(fileName))

  def toString[T <: AkkaStream: TypeTag](akkaGraph: T,
                                         format: TextFormat = TextFormat.Text,
                                         direction: FlowDirection = LeftToRight): String = {
    val graphviz = prepare(akkaGraph, direction)
    if (format == TextFormat.Text) {
      logger.warn(s"Text format support is limited and may not render everything correctly.")
    }

    format.post(graphviz.render(format.asJava))
  }

  def toAbstractGraph[T <: AkkaStream: TypeTag](akkaGraph: T): ScalaGraph = {
    val traversed     = StreamDeconstructorProxy(akkaGraph)
    val graphTypeName = PackageNameSimplifier(typeOf[T].toString)
    traversed.variables().set(properties.graph.GraphLabelKey, graphTypeName)

    traversed
  }

  object properties {
    object graph {
      val GraphLabelKey: String = "stream_label"
    }

    object node {
      val StageName: Key[String]              = Key("stageName")
      val ImplementationName: Key[String]     = Key("implName")
      val StageImplementation: Key[AkkaStage] = Key("stageImpl")
    }

    object edge {
      val Label = "to"
    }

  }

  private def prepare[T <: AkkaStream: TypeTag](akkaGraph: T, direction: FlowDirection): Graphviz = {
    val in  = toAbstractGraph(akkaGraph)
    val out = LowLevelApi.toVizGraph(in)
    Graphviz.fromGraph(out.generalAttr().`with`(direction.asJava).generalAttr().`with`("labelloc", "top"))
  }

  { //initialize GraphViz engine
    Graphviz.useEngine(new GraphvizJdkEngine, new GraphvizV8Engine, new GraphvizCmdLineEngine())
  }
}
