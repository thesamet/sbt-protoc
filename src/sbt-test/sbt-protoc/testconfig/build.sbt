val protobufVersion = "3.4.0"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % protobufVersion % "protobuf"

PB.targets in Compile := Seq(PB.gens.java(protobufVersion) -> (sourceManaged in Compile).value)

PB.targets in Test := Seq(PB.gens.java(protobufVersion) -> (sourceManaged in Test).value)

Project.inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings)

commands ++= List(existsSource, existsClass, existsTestClass, existsTestSource)

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

val existsTestSource: Command = Command.single("existsTestSource"){
  (state: State, path: String) => checkFile(state, sourceManaged in Test, path)
}

val existsClass: Command = Command.single("existsClass"){
  (state: State, path: String) => checkFile(state, classDirectory in Compile, path)
}

val existsTestClass: Command = Command.single("existsTestClass"){
  (state: State, path: String) => checkFile(state, classDirectory in Test, path)
}
