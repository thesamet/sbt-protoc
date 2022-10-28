val protobufVersion = "3.21.7"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % protobufVersion % "protobuf"

configs(IntegrationTest)
Defaults.itSettings

Project.inConfig(IntegrationTest)(sbtprotoc.ProtocPlugin.protobufConfigSettings)

Compile / PB.targets := Seq(PB.gens.java(protobufVersion) -> (Compile / sourceManaged).value)

IntegrationTest / PB.targets := Seq(
  PB.gens.java(protobufVersion) -> (IntegrationTest / sourceManaged).value
)
