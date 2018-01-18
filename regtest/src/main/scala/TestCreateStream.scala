import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.reflect.runtime.universe._

object TestCreateStream extends App {
  val graph = Source.single("t") //.map(_ + "a").via(Flow[String].map(identity)).to(Sink.foreach(println))

//  println(net.mikolak.travesty.registry.lookup(graph))

}
