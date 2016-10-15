libraryDependencies += "com.google.protobuf" % "protobuf-java" % "3.0.0" % "protobuf"

excludeFilter in PB.generate := "test1.proto"

unmanagedResourceDirectories in Compile ++= (PB.protoSources in Compile).value

TaskKey[Unit]("checkJar") := IO.withTemporaryDirectory{ dir =>
  val files = IO.unzip((packageBin in Compile).value, dir, "*.proto")
  val expect = Set("test1.proto", "test2.proto").map(dir / _)
  assert(files == expect, s"$files $expect")
}

// https://github.com/sbt/sbt-protobuf/issues/37
mainClass in compile := Some("whatever")
