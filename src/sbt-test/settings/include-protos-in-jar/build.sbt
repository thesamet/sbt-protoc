val protobufVersion = "3.11.4"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % protobufVersion % "protobuf"

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
