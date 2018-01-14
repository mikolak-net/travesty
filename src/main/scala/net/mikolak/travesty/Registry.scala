package net.mikolak.travesty

import akka.stream.{Graph, Shape}
import net.mikolak.travesty.setup.TravestyConfig

import scala.reflect.runtime.universe._
import scalacache.Id

private[travesty] class Registry(config: TravestyConfig) {
  import scalacache.Cache
  import scalacache.modes.sync._
  import scalacache.caffeine._

  private val cache: Cache[ShapeTypes] = CaffeineCache[ShapeTypes]

  def register[T <: Graph[_ <: Shape, _]: TypeTag](g: T): T = {
    val shapeTypes = deconstructShape(g)
    cache.put(g)(shapeTypes, ttl = Some(config.shapeCacheTtl))
    g
  }

  def lookup(g: Graph[_ <: Shape, _]): Id[Option[ShapeTypes]] = cache.get(g)

  private[travesty] def deconstructShape[T <: Graph[_ <: Shape, _]: TypeTag](g: T): ShapeTypes = {
    val tpe             = typeOf[T]
    val graphType       = tpe.baseType(typeOf[Graph[_, _]].typeSymbol)
    val bottomShapeType = graphType.typeArgs.head

    val shapeType = bottomShapeType.baseClasses.map(bottomShapeType.baseType).head

    val inletSize  = g.shape.inlets.size
    val outletSize = g.shape.outlets.size
    val portTypes  = shapeType.typeArgs

    val typeLists = if (inletSize + outletSize > portTypes.size) {
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

    ShapeTypes.tupled(typeLists)
  }

}

case class ShapeTypes(inlets: List[Type], outlets: List[Type])
