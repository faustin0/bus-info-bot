import Dependencies._
import sbt.Keys.test
import sbtassembly.AssemblyKeys.assembly

inThisBuild(
  List(
    version := "0.1.0-SNAPSHOT",
    organization := "dev.faustin0",
    developers := List(
      Developer("faustin0", "Fausto Di Natale", "", url("https://github.com/faustin0"))
    ),
    homepage := Some(url("https://github.com/faustin0/bus-info-bot")),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    pomIncludeRepository := { _ => false }
  )
)

// General Settings
lazy val commonSettings = Seq(
  scalaVersion := "2.13.5",
  addCompilerPlugin("org.typelevel" %% "kind-projector"     % kindProjectorV cross CrossVersion.full),
  addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % betterMonadicForV),
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-language:postfixOps",
    "-language:higherKinds",
    "-Xfatal-warnings",
    "-Ymacro-annotations"
  ),
  libraryDependencies ++= dependencies ++ testDependencies
)

lazy val root = project
  .in(file("."))
  .settings(
    commonSettings,
    name := "bus-info-bot",
    fork in Test := true
  )
  .settings(
    assemblySetting,
    test in assembly := {},
    assemblyJarName in assembly := "bus-info-bot.jar"
  )

lazy val assemblySetting = assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.last
  case "module-info.class"                                  => MergeStrategy.concat
  case "mime.types"                                         => MergeStrategy.filterDistinctLines
  case s                                                    => MergeStrategy.defaultMergeStrategy(s)
}
