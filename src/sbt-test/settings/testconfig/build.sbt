Project.inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings)

val protobufVersion = "3.11.4"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % protobufVersion % "protobuf"

PB.targets in Compile := Seq(PB.gens.java(protobufVersion) -> (sourceManaged in Compile).value)

PB.targets in Test := Seq(PB.gens.java(protobufVersion) -> (sourceManaged in Test).value)
