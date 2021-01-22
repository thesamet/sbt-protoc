val protobufVersion = "3.11.4"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % protobufVersion % "protobuf"

Compile / PB.targets := Seq(PB.gens.java(protobufVersion) -> (Compile / sourceManaged).value)
Compile / PB.protocOptions := Seq(s"--descriptor_set_out=${target.value}/descriptor.pb")

// append values rather than assigning them so that other plugins can also inject their own
Test / PB.targets += PB.gens.java(protobufVersion) -> (Test / sourceManaged).value
