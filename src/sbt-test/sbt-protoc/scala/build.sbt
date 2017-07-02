import com.trueaccord.scalapb.compiler.Version.protobufVersion

PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value)

libraryDependencies += "com.google.protobuf" % "protobuf-java" % protobufVersion % "protobuf"

mainClass in compile := Some("whatever")
