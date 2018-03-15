package net.mikolak.travesty

import akka.stream.{AmorphousShape, ClosedShape, FlowShape}
import akka.stream.scaladsl.{BidiFlow, Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}
import gremlin.scala.ScalaGraph
import net.mikolak.travesty
import net.mikolak.travesty.properties.node.StageName
import net.mikolak.travesty.setup.Wiring
import org.scalatest.words.MustVerb
import org.scalatest.{FlatSpec, MustMatchers}

import scala.reflect.runtime.universe._

class TravestyToPartialGraphSpec extends FlatSpec with MustMatchers with MustVerb {

  def tested[T <: AkkaStream: TypeTag](g: T): ScalaGraph = travesty.toAbstractGraph(g)

  val InletName  = Wiring.config.partialNames.inlet
  val OutletName = Wiring.config.partialNames.outlet

  "The Stream -> Abstract Graph converter" must "decompose a solo source" in {
    val input = Source.single("t")

    val result = travesty.toAbstractGraph(input)

    val vertices = result.V().toList()

    vertices.map(_.property(StageName).value()) must contain allOf ("singleSource", OutletName)
    vertices must have size 2

    result.E().simplePath().toList() must have size 1
  }

  it must "decompose a solo sink" in {
    val input = Sink.seq[String]

    val result = travesty.toAbstractGraph(input)

    val vertices = result.V().toList()

    vertices.map(_.property(StageName).value()) must contain allOf (InletName, "seqSink")
    vertices must have size 2

    result.E().simplePath().toList() must have size 1
  }

  it must "decompose a solo flow" in {
    val input = Flow[String].map(_ + "a")

    val result = travesty.toAbstractGraph(input)

    val vertices = result.V().toList()

    vertices.map(_.property(StageName).value()) must contain allOf (InletName, "map", OutletName)
    vertices must have size 3

    result.E().simplePath().toList() must have size 2
  }

  it must "decompose partial flow graphs" in {
    val input = Source.single("t").via(Flow[String].map(_ + "a"))

    val result = travesty.toAbstractGraph(input)

    val vertices = result.V().toList()

    vertices.map(_.property(StageName).value()) must contain allOf ("singleSource", "map", OutletName)
    vertices must have size 3

    result.E().simplePath().toList() must have size 2
  }

  it must "decompose a partial two-sink graph" in {
    val input = Source.single("t").alsoTo(Sink.seq)

    val result = tested(input)

    val vertices = result.V().toList()

    vertices.map(_.property(StageName).value()) must contain allOf ("singleSource", "broadcast", "seqSink", OutletName)
    vertices must have size 4

    result.E().simplePath().toList() must have size 3
    result.E().cyclicPath() must not be 'exists

    result.V().has(StageName, "broadcast").outE().toList() must have size 2

  }

  it must "decompose a BidiFlow" in {
    val input = BidiFlow.identity[String, String]

    val result = tested(input)

    val vertices = result.V().toList()

    vertices.map(_.property(StageName).value()) must contain allOf (InletName, "identityOp", OutletName)
    vertices must have size 6

    result.E().simplePath().toList() must have size 4
    result.E().cyclicPath() must not be 'exists
  }

  it must "decompose a composite flow stage" in {
    val input = Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val source = b.add(Source.single("t"))

      val bCast = b.add(Broadcast[String](2))

      val merge = b.add(ZipWith[String, String, String](_ + _))

      val sink = b.add(Sink.seq[String])

      source ~> merge.in0
      merge.out ~> bCast.in
      bCast.out(1) ~> sink

      FlowShape(merge.in1, bCast.out(0))
    })

    val result = tested(input)

    val vertices = result.V().toList()

    vertices.map(_.property(StageName).value()) must contain allOf ("singleSource", "broadcast", "ZipWith2", "seqSink")
    vertices must have size 6

    result.E().simplePath().toList() must have size 5
  }
}
