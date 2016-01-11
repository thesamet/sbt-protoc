import ReleaseTransformations._

sbtPlugin := true

name := "sbt-protoc"

organization := "com.trueaccord.scalapb"

libraryDependencies ++= Seq(
  "com.trueaccord.scalapb" %% "protoc-bridge" % "0.1"
)

scalacOptions += "-target:jvm-1.7"

releasePublishArtifactsAction := PgpKeys.publishSigned.value

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _)),
  setNextVersion,
  commitNextVersion,
  pushChanges,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _))
)

