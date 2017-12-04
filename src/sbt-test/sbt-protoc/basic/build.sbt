val protobufVersion = "3.5.0"

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

// https://github.com/sbt/sbt-protobuf/issues/37
mainClass in compile := Some("whatever")

PB.targets in Compile := Seq(PB.gens.java(protobufVersion) -> (sourceManaged in Compile).value)

commands ++= List(existsSource, existsClass)

def checkFile(state: State, settingKey: SettingKey[File], path: String): State = {
  val extracted = Project.extract(state)
  val targetDir = extracted.get(settingKey)
  val targetFile = targetDir./(path)
  if (!targetFile.exists())
    sys.error(s"File $path does not exist in ${targetDir.getAbsolutePath}")
  state
}

val existsSource: Command = Command.single("existsSource"){
  (state: State, path: String) => checkFile(state, sourceManaged in Compile, path)
}

val existsClass: Command = Command.single("existsClass"){
  (state: State, path: String) => checkFile(state, classDirectory in Compile, path)
}
