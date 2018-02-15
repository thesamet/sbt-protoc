import com.trueaccord.scalapb.compiler.Version.protobufVersion

PB.targets in Compile := Seq(
  PB.gens.go -> (sourceManaged in Compile).value / "golang",
  PB.gens.gateway -> (sourceManaged in Compile).value / "gateway",
  PB.gens.swagger -> (sourceManaged in Compile).value / "swagger"
)

libraryDependencies ++= Seq(
  "com.google.protobuf" % "protobuf-java" % protobufVersion % "protobuf",
  "com.google.api.grpc" % "googleapis-common-protos" % "0.0.3" % "protobuf"
)

mainClass in compile := Some("whatever")

commands ++= List(existsSource, existsClass)

def checkFile(state: State, settingKey: SettingKey[File], path: String): State = {
  val extracted = Project.extract(state)
  val srcManaged = extracted.get(settingKey)
  val targetFile = srcManaged./(path)
  println(s"Checking ${targetFile.getAbsolutePath}")
  if (!targetFile.exists())
    sys.error(s"File $path does not exist in ${srcManaged.getAbsolutePath}")
  state
}

val existsSource: Command = Command.single("existsSource"){
  (state: State, path: String) => checkFile(state, sourceManaged in Compile, path)
}

val existsClass: Command = Command.single("existsClass"){
  (state: State, path: String) => checkFile(state, classDirectory in Compile, path)
}
