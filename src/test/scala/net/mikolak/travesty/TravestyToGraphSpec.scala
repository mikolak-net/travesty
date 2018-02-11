package net.mikolak.travesty

import akka.stream.ClosedShape
import akka.stream.scaladsl.{BidiFlow, Broadcast, Flow, GraphDSL, Keep, RestartSource, RunnableGraph, Sink, Source, ZipWith}
import gremlin.scala._
import net.mikolak.travesty
import org.scalatest.words.MustVerb
import org.scalatest.{FlatSpec, MustMatchers}

import scala.concurrent.duration.Duration
import scala.reflect.runtime.universe._

class TravestyToGraphSpec extends FlatSpec with MustMatchers with MustVerb {

  def tested[T <: AkkaStream: TypeTag](g: T): ScalaGraph = travesty.toAbstractGraph(g)

  import properties.node._

  "The Stream -> Abstract Graph converter" must "decompose a trivial graph" in {
    val input = Source.empty.to(Sink.ignore)

    val result = tested(input)

    val vertices = result.V().toList()

    vertices.map(_.property(ImplementationName).value()) must contain only ("EmptySource", "IgnoreSink")

    val edges = result.E().toList()
    edges must have size 1
    edges.head.inVertex() mustNot be(edges.head.outVertex())
  }

  it must "decompose an almost-trivial, non-useless graph" in {
    val input = Source.single("t").to(Sink.foreach(println))

    val result = tested(input)

    val vertices = result.V().toList()

    vertices.map(_.property(ImplementationName).value()) must contain allOf ("SingleSource", "IgnoreSink")
    vertices must have size 3 //the third is the impl

    result.E().simplePath().toList() must have size 2
    result.E().cyclicPath() must not be 'exists
  }

  it must "decompose a three-stage graph" in {
    val input = Source.single("t").via(Flow[String].map(_ + "a")).to(Sink.seq)

    val result = tested(input)

    val vertices = result.V().toList()

    vertices.map(_.property(ImplementationName).value()) must contain allOf ("SingleSource", "SeqStage")
    vertices.map(_.property(StageName).value()) must contain allOf ("singleSource", "map", "seqSink")
    vertices must have size 3

    result.E().simplePath().toList() must have size 2
  }

  it must "decompose a two-sink graph" in {
    val input = Source.single("t").alsoTo(Sink.seq).to(Sink.ignore)

    val result = tested(input)

    val vertices = result.V().toList()

    vertices.map(_.property(StageName).value()) must contain allOf ("singleSource", "broadcast", "seqSink", "ignoreSink")
    vertices must have size 4

    result.E().simplePath().toList() must have size 3
    result.E().cyclicPath() must not be 'exists

    result.V().has(StageName, "broadcast").outE().toList() must have size 2

  }

  it must "decompose a two-sink graph from graph DSL" in {
    val input = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val source = b.add(Source.single("t"))

      val bCast = b.add(Broadcast[String](2))

      val sink1 = b.add(Sink.seq[String])
      val sink2 = b.add(Sink.ignore)

      source ~> bCast.in
      bCast.out(0) ~> sink1
      bCast.out(1) ~> sink2

      ClosedShape
    })

    val result = tested(input)

    val vertices = result.V().toList()

    vertices.map(_.property(StageName).value()) must contain allOf ("singleSource", "broadcast", "seqSink", "ignoreSink")
    vertices must have size 4

    result.E().simplePath().toList() must have size 3

    result.V().has(StageName, "broadcast").outE().toList() must have size 2
  }

  it must "decompose a bidi-flow graph" in {
    val input = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val source1 = b.add(Source.single("t"))
      val source2 = b.add(Source.empty[String])

      val identityFlow = b.add(BidiFlow.identity[String, String])

      val sink1 = b.add(Sink.seq[String])
      val sink2 = b.add(Sink.ignore)

      source1 ~> identityFlow.in1
      source2 ~> identityFlow.in2
      identityFlow.out1 ~> sink1
      identityFlow.out2 ~> sink2

      ClosedShape
    })

    val result = tested(input)

    val vertices = result.V().toList()

    vertices.map(_.property(StageName).value()) must contain theSameElementsAs List("singleSource",
                                                                                    "lazySource",
                                                                                    "identityOp",
                                                                                    "identityOp",
                                                                                    "seqSink",
                                                                                    "ignoreSink")
    result.E().simplePath().toList() must have size 4
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

    val result = tested(input)

    val vertices = result.V().toList()

    vertices.map(_.property(StageName).value()) must contain allOf ("singleSource", "broadcast", "ZipWith2", "seqSink")
    vertices must have size 4

    result.E().simplePath().toList() must have size 4
    result.V().has(StageName, "singleSource").out().out().out().cyclicPath() must be an 'exists
  }

  it must "decompose a graph with Transform" in {
    val input = Source.single("t").to(Sink.seq).mapMaterializedValue(_ => "k")

    val result = tested(input)

    val vertices = result.V().toList()

    vertices.map(_.property(ImplementationName).value()) must contain allOf ("SingleSource", "SeqStage")
    vertices.map(_.property(StageName).value()) must contain allOf ("singleSource", "seqSink")
    vertices must have size 2

    result.E().simplePath().toList() must have size 1
  }

  it must "respect custom naming of stages" in {
    val input = Source
      .single("t")
      .named("customSource")
      .via(Flow[String].map(_ + "a").named("customFlow"))
      .to(Sink.seq[String].named("customSink"))

    val result = tested(input)

    val vertices = result.V().toList()

    vertices.map(_.property(ImplementationName).value()) must contain allOf ("SingleSource", "SeqStage")
    vertices.map(_.property(StageName).value()) must contain allOf ("customSource", "customFlow", "customSink")
    vertices must have size 3
  }

  it must "differentiate between same-named stages" in {
    val input =
      Source.single("t").via(Flow[String].map(_ + "a").named("map")).via(Flow[String].map(_ + "a").named("map")).to(Sink.seq)

    val result = tested(input)

    val vertices = result.V().toList()
    vertices must have size 4

    result.E().simplePath().toList() must have size 3
  }

  it must "process RestartSources" in {
    val input = RestartSource.withBackoff(Duration.Zero, Duration.Zero, 0.5)(() => Source.single("t")).to(Sink.ignore)

    val result = tested(input)

    result.V().toList() must have size 2
    result.E().toList() must have size 1
  }

  it must "preserve graph names" in {
    import VizGraphProcessor.GraphWithProperties
    import org.scalatest.OptionValues._
    import properties.graph._
    tested(Source.empty[String].to(Sink.ignore)).props(GraphLabelKey).value must be("RunnableGraph[akka.NotUsed]")

    tested(Source.empty[String].toMat(Sink.seq[String])(Keep.right)).props(GraphLabelKey).value must be(
      "RunnableGraph[Future[immutable.Seq[String]]]")
  }

  import properties.edge._

  it must "store registered types in labels with full type information" in {
    import registry._

    val input = Source.single("1").↓.via(Flow[String].map(_.toInt).↓).↓.to(Sink.seq.↓)

    val result = tested(input)

    result.E().toList().map(_.property(Type).value()) must contain theSameElementsAs List(typeOf[java.lang.String], typeOf[Int])
      .map(Some.apply)
  }

  it must "store registered types in labels with type information in the outgoing node" in {
    import registry._

    val input = Source.single("1").↓.via(Flow[String].map(_.toInt).↓).to(Sink.seq)

    val result = tested(input)

    result.E().toList().map(_.property(Type).value()) must contain theSameElementsAs List(typeOf[java.lang.String], typeOf[Int])
      .map(Some.apply)
  }

  it must "store registered types in labels with type information in the incoming node" in {
    import registry._

    val input = Source.single("1").via(Flow[String].map(_.toInt).↓).to(Sink.seq)

    val result = tested(input)

    result.E().toList().map(_.property(Type).value()) must contain theSameElementsAs List(typeOf[java.lang.String], typeOf[Int])
      .map(Some.apply)
  }

}
