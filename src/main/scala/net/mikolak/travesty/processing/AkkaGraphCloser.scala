package net.mikolak.travesty.processing

import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ClosedShape, Graph}
import net.mikolak.travesty.AkkaStream
import net.mikolak.travesty.setup.PartialNamesConfig

private[travesty] class AkkaGraphCloser(nameConfig: PartialNamesConfig) {

  def apply(stream: AkkaStream): Graph[ClosedShape, _] =
    stream match {
      //done like this due to difficulty of catching inner param at this point
      case closed: Graph[ClosedShape, _] @unchecked if closed.shape == ClosedShape =>
        closed
      case _ =>
        val closedGraph = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
          import GraphDSL.Implicits._

          val innerGraph = b.add(stream)

          for (inlet <- innerGraph.inlets) {
            val source = b.add(Source.empty[Nothing].named(nameConfig.inlet))
            source ~> inlet
          }

          for (outlet <- innerGraph.outlets) {
            val sink = b.add(Sink.ignore.named(nameConfig.outlet))
            outlet ~> sink
          }

          ClosedShape
        })

        closedGraph
    }

}
