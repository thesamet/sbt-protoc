val protobufVersion = "3.21.7"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % protobufVersion % "protobuf"

val It = config("it") extend Runtime

configs(It)
inConfig(It)(Defaults.testSettings)

Project.inConfig(It)(sbtprotoc.ProtocPlugin.protobufConfigSettings)

Compile / PB.targets := Seq(PB.gens.java(protobufVersion) -> (Compile / sourceManaged).value)

It / PB.targets := Seq(
  PB.gens.java(protobufVersion) -> (It / sourceManaged).value
)
