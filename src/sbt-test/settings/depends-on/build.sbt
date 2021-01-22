val protobufVersion = "3.11.4"

lazy val root = (project in file("."))
  .dependsOn(a).settings(commonSettings: _*)

lazy val a = (project in file("sub-a"))
  .dependsOn(b).settings(commonSettings: _*)

lazy val b = (project in file("sub-b"))
  .settings(commonSettings: _*)


lazy val commonSettings = Seq[SettingsDefinition](
  Compile / PB.targets := Seq(PB.gens.java -> (Compile / sourceManaged).value),
  Test / PB.targets := Seq(PB.gens.java -> (Test / sourceManaged).value),
  Project.inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings),
  libraryDependencies += "com.google.protobuf" % "protobuf-java" % protobufVersion % "protobuf"
)
