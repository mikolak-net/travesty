import sbt.Keys.fork

lazy val commonSettings = Seq(
  organization := "net.mikolak",
  version in ThisBuild ~= (_ + s"_$akkaVersion"),
  libraryDependencies += akkaDep,
  scalaVersion := "2.12.4",
  resolvers += "indvd00m-github-repo" at "https://raw.githubusercontent.com/indvd00m/maven-repo/master/repository",
  dependencyOverrides += "commons-io" % "commons-io" % "2.4",
  fork in Test := true
)

lazy val akkaVersion        = Option(System.getProperty("akkaVersion")).getOrElse(defaultAkkaVersion)
lazy val defaultAkkaVersion = "2.5.7"

lazy val gremlinVersion = "3.3.0"

lazy val registryPlugin = project
  .in(file("registry_plugin"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang"    % "scala-compiler" % scalaVersion.value,
      "org.scala-lang"    % "scala-reflect"  % scalaVersion.value),
    exportJars := true
  ).dependsOn(travesty)

lazy val registerTest = project
  .in(file("regtest"))
  .settings(commonSettings)
  .dependsOn(travesty, registryPlugin)
  .settings(
    scalacOptions += (artifactPath in (registryPlugin, Compile, packageBin)).map { file =>
      s"-Xplugin:${file.getAbsolutePath}"}.value,
    scalacOptions += "-Xlog-implicits",
    libraryDependencies += "org.scala-lang"    % "scala-reflect"  % scalaVersion.value
//    scalacOptions += "-Xshow-phases"
  )

lazy val travesty = project
  .in(file("."))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "travesty",
    libraryDependencies ++= Seq(
      "guru.nidi"                 % "graphviz-java"       % "0.2.2",
      //TODO: "com.github.mdr" %% "ascii-graphs" % "0.0.3", pending 2.12
      "com.indvd00m.ascii.render" % "ascii-render"        % "1.2.1", //used instead of above^
      "org.scala-lang.modules"    %% "scala-java8-compat" % "0.8.0",
      "org.scala-lang"            % "scala-reflect"       % scalaVersion.value,
      "org.log4s"                 %% "log4s"              % "1.4.0",
      "com.iheart"                %% "ficus"              % "1.4.3"
    ) ++ macwireDeps ++ scalaCacheDeps ++ gremlinDeps ++ testDeps
  )

lazy val akkaDep     = "com.typesafe.akka" %% "akka-stream" % akkaVersion
lazy val macwireDeps = Seq("macros", "util").map("com.softwaremill.macwire" %% _ % "2.3.0")
lazy val gremlinDeps = Seq("com.michaelpollmeier" %% "gremlin-scala" % s"$gremlinVersion.4",
                           "org.apache.tinkerpop" % "tinkergraph-gremlin" % gremlinVersion)
lazy val scalaCacheDeps = Seq("core", "caffeine").map(n => "com.github.cb372" %% s"scalacache-$n" % "0.22.0")

lazy val testDeps = Seq("org.scalatest" %% "scalatest" % "3.0.4", "org.slf4j" % "slf4j-simple" % "1.7.5").map(_ % "test")

lazy val publishSettings = Seq(
//Sonatype OSS stuff (based on https://github.com/xerial/sbt-sonatype )
  publishTo := Some(
    if (isSnapshot.value)
      Opts.resolver.sonatypeSnapshots
    else
      Opts.resolver.sonatypeStaging
  ),
  publishMavenStyle := true,
  licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/mikolak-net/travesty")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/mikolak-net/travesty"),
      "scm:git@github.com:mikolak-net/travesty.git"
    )
  ),
  developers := List(
    Developer(id = "mikolak-net", name = "Mikołaj Koziarkiewicz", email = "", url = url("http://mikolak.net"))
  )
)
