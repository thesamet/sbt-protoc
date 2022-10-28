val protobufVersion = "3.21.7"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % protobufVersion % "protobuf"

Compile / PB.protoSources := Seq((Compile / sourceDirectory).value / "pb")

PB.generate / excludeFilter := "test1.proto"

TaskKey[Unit]("checkJar") := {
  val binary = (Compile / packageBin).value
  IO.withTemporaryDirectory { dir =>
    val files  = IO.unzip(binary, dir, "*.proto")
    val expect = Set("test1.proto", "test2.proto").map(dir / _)
    assert(files == expect, s"$files $expect")
  }
}

Compile / PB.targets := Seq(PB.gens.java(protobufVersion) -> (Compile / sourceManaged).value)
