import scalapb.compiler.Version.protobufVersion

scalaVersion := "2.13.1"

PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value)

libraryDependencies += "com.google.protobuf" % "protobuf-java" % protobufVersion % "protobuf"

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
