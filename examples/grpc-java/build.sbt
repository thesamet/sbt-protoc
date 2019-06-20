val grpcJavaVersion = "1.23.0"

libraryDependencies += "io.grpc" % "protoc-gen-grpc-java" % "1.19.0" asProtocPlugin

PB.targets in Compile := Seq(
  PB.gens.java -> (sourceManaged in Compile).value,
  PB.gens.plugin("grpc-java") -> (sourceManaged in Compile).value,
)

libraryDependencies ++= Seq(
    "io.grpc" % "grpc-all" % grpcJavaVersion,
    "javax.annotation" % "javax.annotation-api" % "1.3.2"  // needed for grpc-java on JDK9
)
