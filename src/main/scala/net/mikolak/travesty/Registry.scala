package net.mikolak.travesty

import akka.stream.{Graph, Shape}
import net.mikolak.travesty.setup.CacheConfig

import scala.reflect.runtime.universe._
import scalacache.Id

class Registry(config: CacheConfig) {
  import scalacache.Cache
  import scalacache.modes.sync._
  import scalacache.caffeine._

  private val cache: Cache[ShapeTypes] = CaffeineCache[ShapeTypes]

  def register[T <: Graph[_ <: Shape, _]: TypeTag](g: T): T = {
    val shapeTypes = Registry.deconstructShape(g)
    cache.put(g)(shapeTypes, ttl = Some(config.shapeCacheTtl))
    g
  }

  def lookup(g: Graph[_ <: Shape, _]): Id[Option[ShapeTypes]] = cache.get(g)

  def registerWithPorts[T <: Graph[_ <: Shape, _]](g: T, ports: List[String]): T = {
    val shapeTypes = Registry.deconstructShapeWithExplicitType(g, ports)
    println("EXECUTED!")
    cache.put(g)(shapeTypes, ttl = Some(config.shapeCacheTtl))
    g
  }

}

case class ShapeTypes(inlets: List[String], outlets: List[String])

object Registry {

  private[travesty] def deconstructShape[T <: Graph[_ <: Shape, _]: TypeTag](g: T): ShapeTypes = {
    val tpe = typeOf[T]
    deconstructShapeWithExplicitType(g, typeToPorts(tpe))
  }

  private[travesty] def typeToPorts(tpe: Type) = {
    val graphType       = tpe.baseType(typeOf[Graph[_, _]].typeSymbol)
    val bottomShapeType = graphType.typeArgs.head

    val shapeType = bottomShapeType.baseClasses.map(bottomShapeType.baseType).head

    shapeType.typeArgs.map(_.toString)
  }

  private def deconstructShapeWithExplicitType(g: Graph[_ <: Shape, _], portTypes: List[String]) = {

    val inletSize  = g.shape.inlets.size
    val outletSize = g.shape.outlets.size

    val output = if (inletSize + outletSize > portTypes.size) {
      if (outletSize > inletSize) {
        val (ins, outsPart) = portTypes.splitAt(inletSize)
        (ins, outsPart ++ List.fill(outletSize - outsPart.size)(outsPart.lastOption.getOrElse(ins.last)))
      } else {
        val (insPart, outs) = portTypes.splitAt(portTypes.length - outletSize)
        (insPart ++ List.fill(inletSize - insPart.size)(insPart.lastOption.getOrElse(outs.last)), outs)
      }

    } else {
      portTypes.splitAt(inletSize)
    }

    ShapeTypes.tupled(output)
  }
}
