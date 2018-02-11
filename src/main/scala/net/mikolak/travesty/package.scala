package net.mikolak

import java.awt.image.BufferedImage

import akka.stream.{ClosedShape, Graph, Shape}
import gremlin.scala._
import net.mikolak.travesty.VizGraphProcessor.AkkaStage
import net.mikolak.travesty.setup.Wiring

import scala.reflect.runtime.universe._

package object travesty {

  type AkkaStream = Graph[_ <: ClosedShape, _]

  def toImage[T <: AkkaStream: TypeTag](akkaGraph: T,
                                        format: ImageFormat,
                                        direction: FlowDirection = LeftToRight): BufferedImage =
    Wiring.api.toImage(akkaGraph, format, direction)

  def toString[T <: AkkaStream: TypeTag](akkaGraph: T,
                                         format: TextFormat = TextFormat.Text,
                                         direction: FlowDirection = LeftToRight): String =
    Wiring.api.toString(akkaGraph, format, direction)

  //noinspection AccessorLikeMethodIsUnit
  def toFile[T <: AkkaStream: TypeTag](akkaGraph: T, format: ImageFormat, direction: FlowDirection = LeftToRight)(
      fileName: String): Unit = Wiring.api.toFile(akkaGraph, format, direction)(fileName)

  def toAbstractGraph[T <: AkkaStream: TypeTag](akkaGraph: T): ScalaGraph = Wiring.api.toAbstractGraph(akkaGraph)

  val LowLevelApi: VizGraphProcessor = Wiring.vizGraphProcessor

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
      val Label                   = "to"
      val Type: Key[Option[Type]] = Key("elementType")
    }

  }

  object registry {
    implicit class RegistryShape[T <: Graph[_ <: Shape, _]: TypeTag](g: T) {
      def register: T = Wiring.registry.register(g)
      def ↓ : T       = register
    }
  }

}
