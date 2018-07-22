package net.mikolak.travesty

import akka.stream.ClosedShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}
import guru.nidi.graphviz.model.{MutableGraph, Graph => VizGraph}
import guru.nidi.graphviz.parse.Parser
import net.mikolak.travesty
import net.mikolak.travesty.render.TypeNameSimplifier
import org.scalatest.words.MustVerb
import org.scalatest.{FlatSpec, MustMatchers}

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe._

class VizGraphProcessorSpec extends FlatSpec with MustMatchers with MustVerb {

  val testInst                                         = new VizGraphProcessor(new TypeNameSimplifier)
  def tested[T <: AkkaStream: TypeTag](t: T): VizGraph = testInst.toVizGraph(travesty.toAbstractGraph(t))

  def calculateResult[T <: AkkaStream: TypeTag](t: T): MutableGraph = Parser.read(tested(t).toString)

  "The Abstract Graph -> Graphviz converter" must "transform a trivial graph" in {
    val input = Source.empty.to(Sink.ignore)

    val output = calculateResult(input)
    output.nodeList() must have size 2
    output.linkList() must have size 1
  }

  it must "decompose a two-sink graph" in {
    val input = Source.single("t").alsoTo(Sink.seq).to(Sink.ignore)

    val output = calculateResult(input)
    output.nodeList() must have size 4
    output.linkList() must have size 3
  }

  it must "decompose a cyclic graph" in {
    val input = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val source = b.add(Source.single("t"))

      val bCast = b.add(Broadcast[String](2))

      val merge = b.add(ZipWith[String, String, String](_ + _))

      val sink = b.add(Sink.seq[String])

      source ~> merge.in0
      merge.out ~> bCast.in
      bCast.out(0) ~> merge.in1
      bCast.out(1) ~> sink

      ClosedShape
    })

    val output = calculateResult(input)
    output.nodeList() must have size 4
    output.linkList() must have size 4
  }

  it must "differentiate between same-named stages" in {
    val input =
      Source.single("t").via(Flow[String].map(_ + "a").named("map")).via(Flow[String].map(_ + "a").named("map")).to(Sink.seq)

    val output = calculateResult(input)

    output.nodeList() must have size 4
    output.linkList() must have size 3
  }

  it must "GraphViz-rank source and sink stages" in {
    val input = Source.single("t").alsoTo(Sink.seq).to(Sink.ignore)

    val output = calculateResult(input)

    val subGraphs = output.graphs().asScala.toList

    subGraphs must have size 3
    val subGraphRanks = subGraphs.flatMap(_.graphAttrs().iterator().asScala.filter(_.getKey == "rank").toList)
    subGraphRanks.map(_.getValue) must contain theSameElementsAs List("source", "sink", "sink")
  }

  it must "preserve materialized names within a graph label" in {
    import VizGraphProcessor.AttrScala
    val input = Source.empty[String].to(Sink.ignore)

    val output = calculateResult(input)

    output.generalAttrs().toMap must contain(("label", "RunnableGraph[akka.NotUsed]"))
  }

  it must "preserve edge types as graph labels" in {
    import registry._

    val input = Source.single("1").↓.via(Flow[String].map(_.toInt).↓).↓.to(Sink.seq.↓)

    val output = calculateResult(input)
    output.linkList().map(_.attrs().iterator().next().getValue) must contain theSameElementsAs List(typeOf[String], typeOf[Int])
      .map(_.toString)
  }

  it must "support unnamed stages" in {
    val input = Source.single("hello")
      .log("logger")
      .to(Sink.head)

    val output = calculateResult(input)

    output.nodeList() must have size 3
    output.linkList() must have size 2
  }

  implicit class MutGraphExtras(g: MutableGraph) {

    def nodeList() = g.nodes().asScala.toList

    def linkList() = nodeList().flatMap(_.links().asScala)

  }
}
