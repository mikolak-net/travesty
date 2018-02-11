package net.mikolak.travesty.setup

import guru.nidi.graphviz.engine.{AbstractGraphvizEngine, GraphvizCmdLineEngine, GraphvizJdkEngine, GraphvizV8Engine}

object GraphvizEngineType extends Enumeration {

  protected case class Val(instantiate: () => AbstractGraphvizEngine) extends super.Val
  implicit def valueToPlanetVal(x: Value): Val = x.asInstanceOf[Val]

  val CommandLine = Val(() => new GraphvizCmdLineEngine())
  val JDK         = Val(() => new GraphvizJdkEngine)
  val V8          = Val(() => new GraphvizV8Engine)

}
