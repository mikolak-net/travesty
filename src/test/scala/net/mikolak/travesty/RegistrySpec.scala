package net.mikolak.travesty

import akka.stream.{Graph, Shape, scaladsl}
import akka.stream.scaladsl.{BidiFlow, Broadcast, Flow, Merge, MergePreferred, Sink, Source, Unzip, Zip, ZipN}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}
import org.scalatest.words.MustVerb

import scala.concurrent.Future
import scala.reflect.runtime.universe._

class RegistrySpec extends FlatSpec with MustMatchers with MustVerb with TableDrivenPropertyChecks {

  val registry = Wiring.registry

  {
    def tested[T <: Graph[_ <: Shape, _]: TypeTag](g: T) = registry.deconstructShape(g)

    "deconstructShape" must "correctly define port types for basic shapes" in {
      tested(Source.empty[String]) must be(ShapeTypes(Nil, List(typeOf[String])))
      tested(Sink.seq[Int]) must be(ShapeTypes(List(typeOf[Int]), Nil))
      tested(Flow[Boolean].map(identity)) must be(ShapeTypes(List(typeOf[Boolean]), List(typeOf[Boolean])))
      tested(Flow[A].map(_.toString)) must be(ShapeTypes(List(typeOf[A]), List(typeOf[java.lang.String])))
    }

    it must "correctly define port types for fanX shapes" in {
      tested(Broadcast[String](4)) must be(ShapeTypes(List(typeOf[String]), List.fill(4)(typeOf[String])))
      tested(Merge[Boolean](7, true)) must be(ShapeTypes(List.fill(7)(typeOf[Boolean]), List(typeOf[Boolean])))

      tested(Zip[A, String]) must be(ShapeTypes(List(typeOf[A], typeOf[String]), List(typeOf[(A, String)])))
      tested(Unzip[A, String]) must be(ShapeTypes(List(typeOf[(A, String)]), List(typeOf[A], typeOf[String])))
      tested(ZipN[A](20)) must be(ShapeTypes(List.fill(20)(typeOf[A]), List(typeOf[scala.collection.immutable.Seq[A]])))
    }

    it must "correctly define ports for uniformFanX shapes" in {
      tested(MergePreferred[B](3, false)) must be(ShapeTypes(List.fill(3 + 1)(typeOf[B]), List(typeOf[B])))
    }

    it must "correctly define port types for BidiFlows" in {
      tested(BidiFlow.fromFunctions[A, B, B, A](_.toB, _.toA)) must be(
        ShapeTypes(List(typeOf[A], typeOf[B]), List(typeOf[B], typeOf[A])))
    }

    it must "correctly define port types for misc shapes" in {
      tested(Flow[A].map(_.toB).async) must be(ShapeTypes(List(typeOf[A]), List(typeOf[B])))

      tested(Flow[A].mapAsync(3)(a => Future.successful(a.toB)).async) must be(ShapeTypes(List(typeOf[A]), List(typeOf[B])))
    }
  }

  {
    def when[T <: Graph[_ <: Shape, _]: TypeTag](g: T) = registry.register(g)

    "register" must "remember shape immediately after saving" in {
      val s = Source.single("t")

      when(s)

      registry.lookup(s) must be a 'nonEmpty
    }

    it must "not remember a non-saved shape" in {
      val s = Source.single("t")

      registry.lookup(s) must be an 'empty
    }

    it must "remember a shape after a short time" in {
      val s = Flow[B].map(identity)

      when(s)

      Thread.sleep(100)

      registry.lookup(s) must be a 'nonEmpty
    }

  }
}

trait A {
  def toB: B

}

trait B {
  def toA: A
}
