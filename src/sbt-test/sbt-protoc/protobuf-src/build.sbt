Project.inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings)

libraryDependencies += "com.google.api.grpc" % "proto-google-common-protos" % "1.17.0" % "protobuf-src" intransitive()

libraryDependencies += "com.google.protobuf" % "protobuf-java" % "3.11.4" % "protobuf"

PB.targets in Compile := Seq(PB.gens.java("3.11.4") -> (sourceManaged in Compile).value)

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
