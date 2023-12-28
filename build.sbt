import Dependencies._

inThisBuild(
  List(
    version              := "0.1.0-SNAPSHOT",
    organization         := "dev.faustin0",
    developers           := List(
      Developer("faustin0", "Fausto Di Natale", "", url("https://github.com/faustin0"))
    ),
    homepage             := Some(url("https://github.com/faustin0/bus-info-bot")),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    pomIncludeRepository := { _ => false }
  )
)

// General Settings
lazy val commonSettings = Seq(
  scalaVersion := "2.13.12",
  addCompilerPlugin("org.typelevel" %% "kind-projector"     % kindProjectorV cross CrossVersion.full),
  addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % betterMonadicForV),
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-language:postfixOps",
    "-language:higherKinds",
    "-Xfatal-warnings"
  ),
  libraryDependencies ++= dependencies ++ testDependencies
)

lazy val root = project
  .in(file("."))
  .enablePlugins(LauncherJarPlugin)
  .settings(
    commonSettings,
    name                    := "bus-info-bot",
    Test / fork             := true,
    Universal / packageName := "bus-info-bot"
  )
