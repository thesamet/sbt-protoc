PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value)

libraryDependencies += "com.google.protobuf" % "protobuf-java" % "3.2.0" % "protobuf"

mainClass in compile := Some("whatever")
