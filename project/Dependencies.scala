import sbt._

object Dependencies {
//  val http4sVersion              = "0.21.3"
  val catsVersion                = "3.5.7"
  val http4sVersion              = "0.23.30"
  val testcontainersScalaVersion = "0.41.5"
  val kindProjectorV             = "0.13.3"
  val betterMonadicForV          = "0.3.1"
  val circeVersion               = "0.14.10"
  val dynamodbVersion            = "2.29.50"
  val log4catsVersion            = "2.7.0"
  val logbackVersion             = "1.2.11"
  val canoeVersion               = "0.6.0"
  val fs2Version                 = "3.11.0"
  val log4j2Version              = "2.24.3"

  lazy val dependencies = Seq(
    "org.typelevel"           %% "cats-effect"                % catsVersion,
    "org.http4s"              %% "http4s-dsl"                 % http4sVersion,
    "org.http4s"              %% "http4s-ember-client"        % http4sVersion,
    "org.http4s"              %% "http4s-circe"               % http4sVersion,
    "io.circe"                %% "circe-generic"              % circeVersion,
    "io.circe"                %% "circe-literal"              % circeVersion,
    "software.amazon.awssdk"   % "dynamodb"                   % dynamodbVersion,
    "org.augustjune"          %% "canoe"                      % canoeVersion,
    "co.fs2"                  %% "fs2-core"                   % fs2Version,
    "org.typelevel"           %% "log4cats-slf4j"             % log4catsVersion,
    "org.apache.logging.log4j" % "log4j-layout-template-json" % log4j2Version % Runtime,
    "org.apache.logging.log4j" % "log4j-api"                  % log4j2Version % Runtime,
    "org.apache.logging.log4j" % "log4j-slf4j-impl"           % log4j2Version % Runtime
  )

  lazy val testDependencies = Seq(
    "org.scalatest"  %% "scalatest"                       % "3.2.19"                   % Test,
    "com.dimafeng"   %% "testcontainers-scala-mockserver" % testcontainersScalaVersion % Test,
    "com.dimafeng"   %% "testcontainers-scala-scalatest"  % testcontainersScalaVersion % Test,
    "org.mock-server" % "mockserver-client-java"          % "5.13.2"                   % Test,
    "org.typelevel"  %% "cats-effect-testing-scalatest"   % "1.6.0"                    % Test,
    "org.typelevel"  %% "cats-effect-laws"                % catsVersion                % Test
  )

}
