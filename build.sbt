import ReleaseTransformations._

organization := "com.thesamet"

name := "sbt-protoc"

description := "SBT plugin for generating code from Protocol Buffer using protoc"

scalacOptions := Seq("-deprecation", "-unchecked", "-Xlint", "-Yno-adapted-args")

scalacOptions += {
  if ((sbtVersion in pluginCrossBuild).value.startsWith("0.13")) "-target:jvm-1.7"
  else "-target:jvm-1.8"
}

libraryDependencies ++= Seq(
  "com.github.os72" % "protoc-jar" % "3.4.0",
  "com.trueaccord.scalapb" %% "protoc-bridge" % "0.3.0-M1"
)

sbtPlugin := true

ScriptedPlugin.scriptedSettings

scriptedBufferLog := false

scriptedLaunchOpts += s"-Dplugin.version=${version.value}"

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

crossSbtVersions := Seq("0.13.16", "1.0.4")
