val protobufVersion = "3.11.4"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % protobufVersion % "protobuf"

configs(IntegrationTest)
Defaults.itSettings

Project.inConfig(IntegrationTest)(sbtprotoc.ProtocPlugin.protobufConfigSettings)

PB.targets in Compile := Seq(PB.gens.java(protobufVersion) -> (sourceManaged in Compile).value)

PB.targets in IntegrationTest := Seq(PB.gens.java(protobufVersion) -> (sourceManaged in IntegrationTest).value)
