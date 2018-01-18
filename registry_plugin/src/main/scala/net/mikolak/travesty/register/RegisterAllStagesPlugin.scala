package net.mikolak.travesty.register

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin, PluginComponent}

class RegisterAllStagesPlugin(val global: Global) extends Plugin {
  val name: String                      = "registerakkastreamstrages"
  val components: List[PluginComponent] = List(new WrapInRegisterComponent(global))
  val description: String               = "register akka stream stages"
}
