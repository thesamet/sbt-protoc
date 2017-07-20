import ReleaseTransformations._

organization := "com.thesamet"

name := "sbt-protoc"

description := "SBT plugin for generating code from Protocol Buffer using protoc"

scalacOptions := Seq("-deprecation", "-unchecked", "-Xlint", "-Yno-adapted-args")

scalacOptions += {
  if (sbtVersion.value.startsWith("0.13")) "-target:jvm-1.7"
  else "-target:jvm-1.8"
}

libraryDependencies ++= Seq(
  "com.github.os72" % "protoc-jar" % "3.3.0",
  "com.trueaccord.scalapb" %% "protoc-bridge" % "0.2.7"
)

sbtPlugin := true

ScriptedPlugin.scriptedSettings.filterNot(_.key.key.label == libraryDependencies.key.label)

// https://github.com/sbt/sbt/issues/3325
libraryDependencies ++= {
  CrossVersion.binarySbtVersion(scriptedSbt.value) match {
    case "0.13" =>
      Seq(
        "org.scala-sbt" % "scripted-sbt" % scriptedSbt.value % scriptedConf.toString,
        "org.scala-sbt" % "sbt-launch" % scriptedSbt.value % scriptedLaunchConf.toString
      )
    case _ =>
      Seq(
        "org.scala-sbt" %% "scripted-sbt" % scriptedSbt.value % scriptedConf.toString,
        "org.scala-sbt" % "sbt-launch" % scriptedSbt.value % scriptedLaunchConf.toString
      )
  }
}

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
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  releaseStepTask(bintrayRelease),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

crossSbtVersions := Seq("0.13.15", "1.0.0-RC2")
