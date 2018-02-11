package net.mikolak.travesty.setup

import akka.stream.StreamDeconstructorProxy
import com.softwaremill.macwire._
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.mikolak.travesty.render.TypeNameSimplifier
import net.mikolak.travesty.{Api, Registry, VizGraphProcessor}
import net.ceedubs.ficus.readers.namemappers.implicits.hyphenCase
import net.ceedubs.ficus.readers.EnumerationReader._

private[travesty] object Wiring {

  lazy val baseConfig = ConfigFactory.load()
  lazy val config     = baseConfig.as[TravestyConfig]("travesty")
  import config.cache
  import config.engines

  lazy val registry                 = wire[Registry]
  lazy val streamDeconstructorProxy = wire[StreamDeconstructorProxy]
  lazy val packageNameSimplifier    = wire[TypeNameSimplifier]
  lazy val vizGraphProcessor        = wire[VizGraphProcessor]
  lazy val api                      = wire[Api]

}
