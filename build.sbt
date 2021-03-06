name := "gymHunter"

version := "1.1.0"

scalaVersion := "2.13.1"

lazy val akkaVersion = "2.5.25"

enablePlugins(DockerPlugin, JavaAppPackaging, ProtobufPlugin)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-http"   % "10.1.9",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.9",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.9",
  "com.enragedginger" %% "akka-quartz-scheduler" % "1.8.1-akka-2.5.x",
  "com.github.tomakehurst" % "wiremock" % "2.25.1" % Test,
  "org.iq80.leveldb" % "leveldb" % "0.9" % Test
)

fork in Test := true

version in Docker := "latest"
dockerExposedPorts := Seq(8080)
dockerBaseImage := "openjdk:11-jre-slim"
dockerExposedVolumes := Seq("/mnt/gymhunter-data")