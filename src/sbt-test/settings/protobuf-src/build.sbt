libraryDependencies += "com.google.api.grpc" % "proto-google-common-protos" % "1.17.0" % "protobuf-src" intransitive()

libraryDependencies += "com.google.protobuf" % "protobuf-java" % "3.11.4" % "protobuf"

PB.targets in Compile := Seq(PB.gens.java("3.11.4") -> (sourceManaged in Compile).value)

javacOptions in Compile ++= Seq("-encoding", "UTF-8")
