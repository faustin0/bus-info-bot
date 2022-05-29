import sbt._

object Dependencies {
//  val http4sVersion              = "0.21.3"
  val catsVersion                = "3.3.12"
  val http4sVersion              = "0.23.12"
  val testcontainersScalaVersion = "0.40.7"
  val kindProjectorV             = "0.13.2"
  val betterMonadicForV          = "0.3.1"
  val circeVersion               = "0.14.2"
  val log4catsVersion            = "2.3.1"
  val dynamodbVersion            = "2.17.196"
  val logbackVersion             = "1.2.11"
  val canoeVersion               = "0.6.0"
  val fs2Version                 = "3.2.7"

  lazy val dependencies = Seq(
    "org.typelevel"         %% "cats-effect"         % catsVersion,
    "org.http4s"            %% "http4s-dsl"          % http4sVersion,
    "org.http4s"            %% "http4s-blaze-client" % http4sVersion,
    "org.http4s"            %% "http4s-circe"        % http4sVersion,
    "io.circe"              %% "circe-generic"       % circeVersion,
    "io.circe"              %% "circe-literal"       % circeVersion,
    "org.typelevel"         %% "log4cats-slf4j"      % log4catsVersion,
    "software.amazon.awssdk" % "dynamodb"            % dynamodbVersion,
    "org.augustjune"        %% "canoe"               % canoeVersion,
    "co.fs2"                %% "fs2-core"            % fs2Version,
    "ch.qos.logback"         % "logback-classic"     % logbackVersion % Runtime
  )

  lazy val testDependencies = Seq(
    "org.scalatest"  %% "scalatest"                       % "3.2.12"                   % Test,
    "com.dimafeng"   %% "testcontainers-scala-mockserver" % testcontainersScalaVersion % Test,
    "com.dimafeng"   %% "testcontainers-scala-scalatest"  % testcontainersScalaVersion % Test,
    "org.mock-server" % "mockserver-client-java"          % "5.13.2"                   % Test,
    "org.typelevel"  %% "cats-effect-testing-scalatest"   % "1.4.0"                    % Test,
    "org.typelevel"  %% "cats-effect-laws"                % catsVersion                % Test
  )

}
