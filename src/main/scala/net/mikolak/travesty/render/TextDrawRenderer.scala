package net.mikolak.travesty.render

import com.indvd00m.ascii.render.{Render, Point => DrawPoint, elements => draw}
import guru.nidi.graphviz.attribute.MutableAttributed
import guru.nidi.graphviz.engine.{Renderer => GraphVizRenderer}
import guru.nidi.graphviz.model.{MutableGraph, MutableNode}
import guru.nidi.graphviz.parse.Parser

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import net.mikolak.travesty.LowLevelApi.AttrScala
import net.mikolak.travesty.properties.node

/**
  * Warning - quickly written, temporary prototype. Proceed with reading at your own peril.
  */
private[travesty] object TextDrawRenderer {

  private val CoordScale = 0.33

  def apply(xdotRenderer: GraphVizRenderer): String = {
    val xdotStr = xdotRenderer.toString

    val drawableVizGraph = Parser.read(xdotStr)

    val allNodes = drawableVizGraph.nodes().asScala ++ drawableVizGraph
      .graphs()
      .asScala
      .flatMap(_.nodes().asScala)
    val allEdges = allNodes.flatMap(_.links().asScala).toSet

    def createCanvas(g: MutableGraph) = {
      val Array(x, y, width, height) = drawableVizGraph.graphAttrs().toMap("bb").split(",").map(_.toCoord)

      val textRenderer = new Render()
      val canvas       = textRenderer.newBuilder()
      (textRenderer, canvas.width(width - x).height(height - y))
    }

    val (textRenderer, canvas) = createCanvas(drawableVizGraph)

    for {
      edge <- allEdges
    } {
      val rawPoints =
        edge.attrs().skipToInstruction("draw", "B").drop(1)

      //edge shape
      createPolygon(rawPoints).foreach(canvas.element)

      //edge labels
      for (edgeType <- List("h", "t")) {
        val edgeRawPoints = edge.attrs().skipToInstruction(s"${edgeType}draw", "P").drop(1)
        createPolygon(edgeRawPoints).foreach(canvas.element)
      }
    }

    for {
      node <- allNodes if node.attrs().asScala.nonEmpty //subgraphs have empty attrs, but are important for edges
    } {
      { //node shape
        val Array(x0, y0, w, h) = node.attrs().skipToInstruction("draw", "e").map(_.toCoord)
        canvas.element(new draw.Ellipse(x0, y0, w, h))
      }

      { //node label
        canvas.element(createLabel(node.attrs()))
      }
    }

    //draw graph label
    canvas.element(createLabel(drawableVizGraph.graphAttrs()))

    textRenderer.render(canvas.build()).getText
  }

  private def createLabel(attrs: MutableAttributed[_]) = {
    val textConfig                    = attrs.skipToInstruction("ldraw", "T")
    val Array(x, yBaseline, j, width) = textConfig.take(4).map(_.toCoord)
    val text                          = textConfig.last.tail

    new draw.Label(text, x - (text.length / 2), yBaseline + 1)
  }

  private def createPolygon(rawCoords: Array[String]): Iterator[draw.Line] = {
    val coords = rawCoords.map(_.toCoord).grouped(2).toList

    for {
      Seq(Array(x1, y1), Array(x2, y2)) <- coords.sliding(2)
    } yield {
      new draw.Line((x1, y1), (x2, y2))
    }
  }

  private implicit class DrawableAttrs[T](attrSource: MutableAttributed[T]) {
    def skipToInstruction(attr: String, code: String): Array[String] =
      attrSource.toMap.get(s"_${attr}_").map(_.split(" ").dropWhile(_ != code).tail).getOrElse(Array.empty[String])
  }

  private implicit class ScaledIntString(val s: String) extends AnyVal {

    def toCoord: Int = (s.toDouble * CoordScale).ceil.toInt
  }

  private implicit def tupleToPoint(t: (Int, Int)): DrawPoint = (new DrawPoint(_, _)).tupled(t)

}
