import sbt._

object Dependencies {
//  val http4sVersion              = "0.21.3"
  val http4sVersion              = "0.21.15"
  val testcontainersScalaVersion = "0.38.8"
  val catsVersion                = "2.3.1"
  val kindProjectorV             = "0.11.3"
  val betterMonadicForV          = "0.3.1"
  val circeVersion               = "0.13.0"
  val log4catsVersion            = "1.1.1"
  val dynamodbVersion            = "1.11.939"
  val logbackVersion             = "1.2.3"
  val canoeVersion               = "0.5.1"
  val fs2Version                 = "2.5.0"

  lazy val dependencies = Seq(
    "org.typelevel"     %% "cats-effect"           % catsVersion,
    "org.http4s"        %% "http4s-dsl"            % http4sVersion,
    "org.http4s"        %% "http4s-blaze-client"   % http4sVersion,
    "org.http4s"        %% "http4s-circe"          % http4sVersion,
    "io.circe"          %% "circe-generic"         % circeVersion,
    "io.circe"          %% "circe-literal"         % circeVersion,
    "io.chrisdavenport" %% "log4cats-core"         % log4catsVersion,
    "io.chrisdavenport" %% "log4cats-slf4j"        % log4catsVersion,
    "com.amazonaws"      % "aws-java-sdk-dynamodb" % dynamodbVersion,
    "org.augustjune"    %% "canoe"                 % canoeVersion,
    "co.fs2"            %% "fs2-core"              % fs2Version,
    "ch.qos.logback"     % "logback-classic"       % logbackVersion % Runtime
  )

  lazy val testDependencies = Seq(
    "org.scalatest"  %% "scalatest"                       % "3.2.3"                    % Test,
    "com.dimafeng"   %% "testcontainers-scala-dynalite"   % testcontainersScalaVersion % Test,
    "com.dimafeng"   %% "testcontainers-scala-mockserver" % testcontainersScalaVersion % Test,
    "com.dimafeng"   %% "testcontainers-scala-scalatest"  % testcontainersScalaVersion % Test,
    "org.mock-server" % "mockserver-client-java"          % "5.11.2"                   % Test,
    "com.codecommit" %% "cats-effect-testing-scalatest"   % "0.5.0"                    % Test,
    "org.typelevel"  %% "cats-effect-laws"                % catsVersion                % Test
  )
}
