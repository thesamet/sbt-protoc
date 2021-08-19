Compile / PB.targets := Seq(
  PB.gens.java                -> (Compile / sourceManaged).value,
  PB.gens.plugin("grpc-java") -> (Compile / sourceManaged).value,
  (
    PB.gens.plugin("scala"),
    Seq()
  ) -> (Compile / sourceManaged).value,
  (
    PB.gens.plugin("scala"),
    Seq("flat_package")
  ) -> (Compile / sourceManaged).value
)

val isWindows               = protocbridge.SystemDetector.detectedClassifier().startsWith("windows")
val protocGenScalaExtension = if (isWindows) "bat" else "sh"
val protocGenScalaClassifier = if (isWindows) "windows" else "unix"

libraryDependencies += "javax.annotation" % "javax.annotation-api" % "1.3.2"
libraryDependencies += "io.grpc"          % "grpc-stub"            % "1.35.0"
libraryDependencies += "io.grpc"          % "grpc-protobuf"        % "1.35.0"
libraryDependencies += "io.grpc"          % "protoc-gen-grpc-java" % "1.35.0" asProtocPlugin ()

libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" % "0.10.10"
libraryDependencies += "com.thesamet.scalapb" % "protoc-gen-scala" % "0.10.10" % "protobuf" artifacts (
  Artifact(
    "protoc-gen-scala",
    PB.ProtocPlugin,
    protocGenScalaExtension,
    protocGenScalaClassifier
  )
)
