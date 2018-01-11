package net.mikolak.travesty

import akka.stream.scaladsl.{Sink, Source}
import net.mikolak.travesty.OutputFormat.{PNG, SVG}
import net.mikolak.travesty.TextFormat.{JSON, Text, XDOT}
import org.scalatest.{FlatSpec, MustMatchers}
import org.scalatest.words.MustVerb

class TravestyToOutputSpec extends FlatSpec with MustMatchers with MustVerb {

  //smoke tests for renderers
  val graph = Source.single("t").map(_ + "a").to(Sink.foreach(println))

  for (format <- List(PNG, SVG)) {
    it must s"generate an image for the $format format" in {
      noException must be thrownBy { toImage(graph, format) }
    }
  }

  for (format <- List(XDOT, JSON, Text)) {
    it must s"generate text for the $format format" in {
      noException must be thrownBy { net.mikolak.travesty.toString(graph, format) }
    }
  }

}
