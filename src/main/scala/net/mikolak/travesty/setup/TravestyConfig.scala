package net.mikolak.travesty.setup

import scala.concurrent.duration.FiniteDuration

private[travesty] case class TravestyConfig(cache: CacheConfig,
                                            engines: List[GraphvizEngineType.Value],
                                            partialNames: PartialNamesConfig,
                                            render: RenderConfig)

private[travesty] case class CacheConfig(shapeCacheTtl: FiniteDuration)

private[travesty] case class PartialNamesConfig(inlet: String, outlet: String)

/**
 *
 * @param defaultStageName will be use whenever a stage name cannot be inferred
 */
private[travesty] case class RenderConfig(defaultStageName: String)
