val protobufVersion = "3.11.4"

lazy val root = (project in file("."))
  .dependsOn(a).settings(commonSettings: _*)

lazy val a = (project in file("sub-a"))
  .dependsOn(b).settings(commonSettings: _*)

lazy val b = (project in file("sub-b"))
  .settings(commonSettings: _*)


lazy val commonSettings = Seq[SettingsDefinition](
  PB.targets in Compile := Seq(PB.gens.java -> (sourceManaged in Compile).value),
  PB.targets in Test := Seq(PB.gens.java -> (sourceManaged in Test).value),
  Project.inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings),
  libraryDependencies += "com.google.protobuf" % "protobuf-java" % protobufVersion % "protobuf"
)

def checkFile(state: State, settingKey: SettingKey[File], path: String): State = {
  val extracted = Project.extract(state)
  val targetDir = extracted.get(settingKey)
  val targetFile = targetDir./(path)
  if (!targetFile.exists())
    sys.error(s"File $path does not exist in ${targetDir.getAbsolutePath}")
  state
}


def makeExistsCommand(name: String, key: SettingKey[File]): Command =
  Command.single(name){
    (state: State, path: String) => checkFile(state, key, path)
  }

val existsSource: Command = makeExistsCommand("existsSource", sourceManaged in Compile)
val existsASource: Command = makeExistsCommand("existsASource", sourceManaged in (a, Compile))
val existsBSource: Command = makeExistsCommand("existsBSource", sourceManaged in (b, Compile))

val existsClass: Command = makeExistsCommand("existsClass", classDirectory in Compile)
val existsAClass: Command = makeExistsCommand("existsAClass", classDirectory in (a, Compile))
val existsBClass: Command = makeExistsCommand("existsBClass", classDirectory in (b, Compile))

commands ++= List(existsSource, existsClass, existsAClass, existsASource, existsBClass, existsBSource)
