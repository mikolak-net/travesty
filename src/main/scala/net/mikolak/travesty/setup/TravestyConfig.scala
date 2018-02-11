package net.mikolak.travesty.setup

import scala.concurrent.duration.FiniteDuration

private[travesty] case class TravestyConfig(cache: CacheConfig, engines: List[GraphvizEngineType.Value])

private[travesty] case class CacheConfig(shapeCacheTtl: FiniteDuration)
