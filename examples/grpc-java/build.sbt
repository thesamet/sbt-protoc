val grpcJavaVersion = "1.81.0"

libraryDependencies += ("io.grpc" % "protoc-gen-grpc-java" % grpcJavaVersion) asProtocPlugin ()

Compile / PB.targets := Seq(
  PB.gens.java                -> (Compile / sourceManaged).value,
  PB.gens.plugin("grpc-java") -> (Compile / sourceManaged).value
)

libraryDependencies ++= Seq(
  "io.grpc"          % "grpc-all"             % grpcJavaVersion,
  "javax.annotation" % "javax.annotation-api" % "1.3.2"
)
