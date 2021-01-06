val protobufVersion = "3.11.4"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % protobufVersion % "protobuf"

PB.targets in Compile := Seq(PB.gens.java(protobufVersion) -> (sourceManaged in Compile).value)
PB.protocOptions in Compile := Seq(s"--descriptor_set_out=${target.value}/descriptor.pb")

// append values rather than assigning them so that other plugins can also inject their own
PB.targets in Test += PB.gens.java(protobufVersion) -> (sourceManaged in Test).value
