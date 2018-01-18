package net.mikolak.travesty.register

import akka.stream.{ClosedShape, Graph, Shape}

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.Transform

class WrapInRegisterComponent(val global: Global) extends PluginComponent with Transform {
  protected def newTransformer(unit: global.CompilationUnit) = WrapInRegisterTransformer

  val runsAfter: List[String] = List("typer")
  val phaseName: String       = WrapInRegisterComponent.Name

  import global._

  object WrapInRegisterTransformer extends Transformer {

    override def transform(tree: global.Tree) = {
      val transformed = super.transform(tree)
      transformed match {
        case call @ Apply(_, _) =>
          if (call.tpe != null && call.tpe.finalResultType <:< typeOf[akka.stream.Graph[_ <: Shape, _]] && !(call.tpe.finalResultType <:< typeOf[
                akka.stream.Graph[_ <: ClosedShape, _]])) {
            println("----")
            println(call.getClass)
            println(showRaw(call))
            println(s"Found akka expr $call")

            println(showRaw(reify(net.mikolak.travesty.registry)))

            val reg = reify(net.mikolak.travesty.registry)

//            this resolves to the Registry class type for some reason
//            val reg = symbolOf[net.mikolak.travesty.registry.type]

            val graphType = call.tpe.baseType(typeOf[Graph[_, _]].typeSymbol)

            if (graphType.typeArgs.nonEmpty) {
              val ports = {
                val bottomShapeType = graphType.typeArgs.head

                val shapeType = bottomShapeType.baseClasses.map(bottomShapeType.baseType).head

                shapeType.typeArgs.map(_.toString)
              }

              // resolves to EmptyTree for some reason
              //            val typeTag =
              //              typer.resolveTypeTag(call.pos, NoType, call.tpe.finalResultType, true)
              //            println(s"TYPETAG: $typeTag")

              val listSymbOf = symbolOf[List.type]
              val finalPorts = q"$listSymbOf.apply(..$ports)"

              println("PORTDEF TYPED")
              println(showRaw(typer.typed(finalPorts)))

              println("WRAPPED CALL UNTYPED")
              val wrappedCall = q"$reg.registerWithPorts[${call.tpe.finalResultType}]($call, $finalPorts)"
              println(showRaw(wrappedCall))
              println(show(wrappedCall))

              println("WRAPPED CALL TYPED")
              val ret = typer.typed(wrappedCall)
              println(showRaw(ret))
              println(show(ret))
              println("----")
              ret
            } else {
              call
            }
          } else {
            call
          }
        case _ => transformed
      }
    }
  }
}

object WrapInRegisterComponent {
  val Name = "wrap_in_register_component"
}
