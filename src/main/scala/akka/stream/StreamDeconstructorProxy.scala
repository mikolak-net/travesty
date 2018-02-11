package akka.stream

import akka.stream.Attributes.Name
import akka.stream.impl.StreamLayout.AtomicModule
import akka.stream.impl._
import akka.stream.impl.fusing.GraphStageModule
import gremlin.scala._
import net.mikolak.travesty.{AkkaStream, Registry, properties}
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.log4s._

/**
  * INTERNAL API
  * Package placement necessary to access package private Traversal.
  */
class StreamDeconstructorProxy(typeRegistry: Registry) {

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

    var edgeConstructions = List.empty[(ScalaVertex, Int, Int)]

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
        edgeConstructions ++= module.shape.outlets.zipWithIndex.map { case (o, i) => (currentNode, i, inOffset + slots(o.id)) }
      case Pop | PushNotUsed           => //supported, no-op
      case EnterIsland(_) | ExitIsland => //supported, no-op (FIXME: process)
      case Compose(_, _)               => //supported, no-op currently (FIXME: possibly include mat virtual nodes)
      case Transform(_)                => //supported, no-op currently (FIXME: needs to be processed for #2)
    }

    edgeConstructions.foreach {
      case (outNode, outIndex, inNodeIndex) =>
        val inNode = inputMap(inNodeIndex)

        val List(inTypes, outTypes) = List(inNode, outNode)
          .map(_.property(properties.node.StageImplementation).value())
          .map(typeRegistry.lookup)

        //inlets are ordered sequentially in the "master" map, so all we need to get the target inlet's index
        // is to find the number of keys mapping to the same node
        val inletIndex = inputMap.count { case (index, node) => node == inNode && index < inNodeIndex }

        val typeLabel = outTypes.map(_.outlets(outIndex)).orElse(inTypes.map(_.inlets(inletIndex)))

        outNode --- (properties.edge.Label, properties.edge.Type -> typeLabel) --> inNode.asJava()
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
