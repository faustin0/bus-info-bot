import sbt._

object Dependencies {
  val http4sVersion = "0.21.9"

  val testcontainersScalaVersion = "0.38.6"

  val catsVersion = "2.2.0"

  val kindProjectorV = "0.11.0"

  val betterMonadicForV = "0.3.1"

  val circeVersion = "0.13.0"

  val log4catsVersion = "1.1.1"

  val dynamodbVersion = "1.11.903"

  val logbackVersion = "1.2.3"

  val canoeVersion = "0.5.1"

  lazy val dependencies = Seq(
    "org.typelevel"     %% "cats-effect"           % catsVersion,
    "org.http4s"        %% "http4s-dsl"            % http4sVersion,
    "org.http4s"        %% "http4s-blaze-client"   % http4sVersion,
    "org.http4s"        %% "http4s-circe"          % http4sVersion,
    "io.circe"          %% "circe-generic"         % circeVersion,
    "io.chrisdavenport" %% "log4cats-slf4j"        % log4catsVersion,
    "com.amazonaws"      % "aws-java-sdk-dynamodb" % dynamodbVersion,
    "org.augustjune"    %% "canoe"                 % canoeVersion,
    "ch.qos.logback"     % "logback-classic"       % logbackVersion % Runtime
  )

  lazy val testDependencies = Seq(
    "org.scalatest"  %% "scalatest"                      % "3.2.3"                    % Test,
    "com.dimafeng"   %% "testcontainers-scala-dynalite"  % testcontainersScalaVersion % Test,
    "com.dimafeng"   %% "testcontainers-scala-scalatest" % testcontainersScalaVersion % Test,
    "com.codecommit" %% "cats-effect-testing-scalatest"  % "0.4.2"                    % Test,
    "org.typelevel"  %% "cats-effect-laws"               % catsVersion                % Test
  )
}
