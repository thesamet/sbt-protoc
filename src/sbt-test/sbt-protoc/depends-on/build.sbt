val protobufVersion = "3.3.1"

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
