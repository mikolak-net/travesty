package akka.stream

import akka.stream.Attributes.Name
import akka.stream.impl.StreamLayout.AtomicModule
import akka.stream.impl._
import akka.stream.impl.fusing.GraphStageModule
import gremlin.scala._
import net.mikolak.travesty.{AkkaStream, properties}
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.log4s._

/**
  * INTERNAL API
  * Package placement necessary to access package private Traversal.
  */
object StreamDeconstructorProxy {

  private val logger = getLogger

  def apply(akkaGraph: AkkaStream): ScalaGraph = processTraversal(akkaGraph.traversalBuilder.traversal)

  def logGraph(traversal: Traversal): Unit =
    logger.trace("Traversal to stdout:\n" + PrintTraversalAccessor(traversal))

  private def processTraversal(traversal: Traversal): ScalaGraph = {
    logGraph(traversal)
    val graph = TinkerGraph.open.asScala

    nodesFrom(traversal, graph)
  }

  private def nodesFrom(traversal: Traversal, graph: ScalaGraph): ScalaGraph = {
    var inputMap                 = Map.empty[Int, ScalaVertex]
    var attributeStack           = List.empty[Attributes]
    var currentNode: ScalaVertex = null

    var edgeConstructions = List.empty[(ScalaVertex, Int)]

    foldTraversal(traversal).foreach {
      case PushAttributes(attributes) => attributeStack = attributes :: attributeStack
      case PopAttributes              =>
        //assign name to the node and pop the stack
        attributeStack.head.attributeList
          .collect {
            case Name(name) => name
          }
          .foreach(currentNode.setProperty(properties.node.StageName, _))

        attributeStack = attributeStack.tail
      case MaterializeAtomic(module, slots) =>
        currentNode = graph.addVertex()
        currentNode.setProperty(properties.node.ImplementationName, module.baseName)
        currentNode.setProperty(properties.node.StageImplementation, module)
        val inOffset = inputMap.size
        //add the inputs per spec
        (inOffset until (inOffset + module.shape.inlets.length)).foreach(i => inputMap += (i -> currentNode))

        //map outputs to existing inputs
        edgeConstructions ++= module.shape.outlets.map(o => (currentNode, inOffset + slots(o.id)))
      case Pop | PushNotUsed           => //supported, no-op
      case EnterIsland(_) | ExitIsland => //supported, no-op (FIXME: process)
      case Compose(_, _)               => //suported, no-op currently (FIXME: possibly include mat virtual nodes)
    }

    edgeConstructions.foreach {
      case (outNode, inNodeIndex) => outNode --- properties.edge.Label --> inputMap(inNodeIndex).asJava()
    }
    graph
  }

  private implicit class DebuggableModule[S <: Shape](m: AtomicModule[S, _]) {

    def baseName: String =
      m match {
        case GraphStageModule(_, _, stage) =>
          val strName = stage.toString

          val sep              = '$'
          val GraphStagePrefix = "akka.stream.impl.fusing.GraphStages"

          if (strName.startsWith(GraphStagePrefix)) {
            strName.split(sep).dropRight(1).last
          } else {
            strName.toString.reverse.takeWhile(_ != '.').reverse
          }

        case _ => m.toString
      }
  }

  /**
    * The linked list structure is nice and efficient, but readability's more important here.
    */
  private def foldTraversal(t: Traversal): List[Traversal] =
    t match {
      case Concat(first, next) => foldTraversal(first) ++ foldTraversal(next)
      case other               => List[Traversal](other)
    }
}
