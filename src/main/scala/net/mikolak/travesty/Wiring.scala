package net.mikolak.travesty

import akka.stream.StreamDeconstructorProxy
import com.softwaremill.macwire._
import net.mikolak.travesty.render.PackageNameSimplifier

private[this] object Wiring {

  lazy val streamDeconstructorProxy = wire[StreamDeconstructorProxy]
  lazy val packageNameSimplifier    = wire[PackageNameSimplifier]
  lazy val vizGraphProcessor        = wire[VizGraphProcessor]
  lazy val api                      = wire[Api]

}
