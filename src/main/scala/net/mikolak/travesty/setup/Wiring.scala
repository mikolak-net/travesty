package net.mikolak.travesty.setup

import akka.stream.StreamDeconstructorProxy
import com.softwaremill.macwire._
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.mikolak.travesty.render.PackageNameSimplifier
import net.mikolak.travesty.{Api, Registry, VizGraphProcessor}
import net.ceedubs.ficus.readers.namemappers.implicits.hyphenCase

private[travesty] object Wiring {

  lazy val baseConfig = ConfigFactory.load()
  lazy val config     = baseConfig.as[TravestyConfig]("travesty")
  import config.cache

  lazy val registry                 = wire[Registry]
  lazy val streamDeconstructorProxy = wire[StreamDeconstructorProxy]
  lazy val packageNameSimplifier    = wire[PackageNameSimplifier]
  lazy val vizGraphProcessor        = wire[VizGraphProcessor]
  lazy val api                      = wire[Api]

}
