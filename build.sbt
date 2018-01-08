organization := "net.mikolak"
name := "travesty"
version := "0.2.15-SNAPSHOT"

scalaVersion := "2.12.4"

resolvers += "indvd00m-github-repo" at "https://raw.githubusercontent.com/indvd00m/maven-repo/master/repository"

libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-stream" % "2.5.7",
  "guru.nidi" % "graphviz-java" % "0.2.2",
  "com.michaelpollmeier" %% "gremlin-scala" % "3.3.0.4",
  "org.apache.tinkerpop" % "tinkergraph-gremlin" % "3.3.0",
  //TODO: "com.github.mdr" %% "ascii-graphs" % "0.0.3", pending 2.12
  "com.indvd00m.ascii.render" % "ascii-render" % "1.2.1", //used instead of above^
  "org.log4s" %% "log4s" % "1.4.0") ++
Seq("org.scalatest" %% "scalatest" % "3.0.4",
"org.slf4j" % "slf4j-simple" % "1.7.5").map(_ % "test")

fork in Test := true