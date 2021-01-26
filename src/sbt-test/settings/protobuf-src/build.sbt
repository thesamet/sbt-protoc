libraryDependencies += "com.google.api.grpc" % "proto-google-common-protos" % "1.17.0" % "protobuf-src" intransitive()

libraryDependencies += "com.google.protobuf" % "protobuf-java" % "3.11.4" % "protobuf"

Compile / PB.targets := Seq(PB.gens.java("3.11.4") -> (Compile / sourceManaged).value)
Test / PB.targets := Seq(PB.gens.java("3.11.4") -> (Test / sourceManaged).value)

Compile / javacOptions ++= Seq("-encoding", "UTF-8")
