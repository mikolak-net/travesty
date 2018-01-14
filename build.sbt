organization := "net.mikolak"
name := "travesty"
version in ThisBuild ~= (_ + s"_$akkaVersion")

lazy val akkaVersion        = Option(System.getProperty("akkaVersion")).getOrElse(defaultAkkaVersion)
lazy val defaultAkkaVersion = "2.5.7"

scalaVersion := "2.12.4"

resolvers += "indvd00m-github-repo" at "https://raw.githubusercontent.com/indvd00m/maven-repo/master/repository"

libraryDependencies ++= Seq(
  "com.typesafe.akka"    %% "akka-stream"        % akkaVersion,
  "guru.nidi"            % "graphviz-java"       % "0.2.2",
  "com.michaelpollmeier" %% "gremlin-scala"      % "3.3.0.4",
  "org.apache.tinkerpop" % "tinkergraph-gremlin" % "3.3.0",
  //TODO: "com.github.mdr" %% "ascii-graphs" % "0.0.3", pending 2.12
  "com.indvd00m.ascii.render" % "ascii-render"        % "1.2.1", //used instead of above^
  "com.github.cb372" %% "scalacache-core" % "0.22.0",
  "com.github.cb372" %% "scalacache-caffeine" % "0.22.0",
  "org.scala-lang.modules"    %% "scala-java8-compat" % "0.8.0",
  "org.scala-lang"            % "scala-reflect"      % scalaVersion.value,
  "org.log4s"                 %% "log4s"              % "1.4.0"
) ++
  Seq("org.scalatest" %% "scalatest" % "3.0.4", "org.slf4j" % "slf4j-simple" % "1.7.5").map(_ % "test")

dependencyOverrides += "commons-io" % "commons-io" % "2.4"

fork in Test := true


//Sonatype OSS stuff (based on https://github.com/xerial/sbt-sonatype )
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

publishMavenStyle := true

licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
homepage := Some(url("https://github.com/mikolak-net/travesty"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/mikolak-net/travesty"),
    "scm:git@github.com:mikolak-net/travesty.git"
  )
)
developers := List(
  Developer(id="mikolak-net", name="Mikołaj Koziarkiewicz", email="", url=url("http://mikolak.net"))
)