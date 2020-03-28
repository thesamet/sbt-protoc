val protobufVersion = "3.11.4"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % protobufVersion % "protobuf"

excludeFilter in PB.generate := "test1.proto"

unmanagedResourceDirectories in Compile ++= (PB.protoSources in Compile).value

TaskKey[Unit]("checkJar") := {
  val binary = (packageBin in Compile).value
  IO.withTemporaryDirectory{ dir =>
    val files = IO.unzip(binary, dir, "*.proto")
    val expect = Set("test1.proto", "test2.proto").map(dir / _)
    assert(files == expect, s"$files $expect")
  }
}

PB.targets in Compile := Seq(PB.gens.java(protobufVersion) -> (sourceManaged in Compile).value)
