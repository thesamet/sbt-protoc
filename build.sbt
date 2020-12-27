import ReleaseTransformations._

organization := "com.thesamet"

name := "sbt-protoc"

description := "SBT plugin for generating code from Protocol Buffer using protoc"

scalacOptions := Seq("-deprecation", "-unchecked", "-Xlint", "-Yno-adapted-args")

scalacOptions += "-target:jvm-1.8"

scalaVersion := "2.12.12"

addSbtPlugin("org.portable-scala" % "sbt-platform-deps" % "1.0.0")

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "protoc-bridge" % "0.9.0-RC8"
)

enablePlugins(SbtPlugin)

scriptedBufferLog := false

scriptedLaunchOpts += s"-Dplugin.version=${version.value}"

// https://github.com/sbt/sbt/issues/5049#issuecomment-538404839
pluginCrossBuild / sbtVersion := "1.2.8"

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

publishMavenStyle := false

bintrayRepository := "sbt-plugins"

bintrayOrganization in bintray := None

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("^ test"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("^ publish"),
  releaseStepTask(bintrayRelease),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
