val protobufVersion = "3.3.1"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % protobufVersion % "protobuf"

mainClass in compile := Some("whatever")

commands ++= List(existsSource, existsClass)

PB.targets in Compile := Seq(PB.gens.go -> (sourceManaged in Compile).value)

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
