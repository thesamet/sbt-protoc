import scalapb.compiler.Version.scalapbVersion

val GrpcProtosArtifact = "com.google.api.grpc" % "grpc-google-common-protos" % "1.17.0"

scalaVersion := "2.12.13"

// This sub-project will hold the compiled Scala classes from the external
// jar.
lazy val googleCommonProtos = (project in file("google-common-protos"))
  .settings(
    name := "google-common-protos",

    // Dependencies marked with "protobuf" get extracted to target / protobuf_external
    libraryDependencies ++= Seq(
      GrpcProtosArtifact % "protobuf"
    ),

    // In addition to the JAR we care about, the protobuf_external directory
    // is going to contain protos from Google's standard protos.  
    // In order to avoid compiling things we don't use, we restrict what's
    // compiled to a subdirectory of protobuf_external
    PB.protoSources in Compile += target.value / "protobuf_external" / "google" / "type",

    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    )
  )

// This sub-project is where your code goes. It contains proto file that imports a proto
// from the external proto jar.
lazy val myProject = (project in file("my-project"))
  .settings(
    name := "my-project",

    // The protos in this sub-project depend on the protobufs in
    // GrpcProtosArtifact, so we need to have them extracted here too. This
    // time we do not add them to `PB.protoSources` so they do not compile.
    libraryDependencies ++= Seq(
      GrpcProtosArtifact % "protobuf"
    ),

    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    ),

  )
  .dependsOn(googleCommonProtos)  // brings the compiled Scala classes from googleCommonProtos
