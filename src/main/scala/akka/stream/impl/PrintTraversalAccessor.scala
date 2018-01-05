package akka.stream.impl

private[stream] object PrintTraversalAccessor {

  def apply(t: Traversal) = TraversalBuilder.printTraversal(t)

}
