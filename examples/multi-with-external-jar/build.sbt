import scalapb.compiler.Version.scalapbVersion

lazy val externalProtos = (project in file("external-protos"))
  .settings(
    libraryDependencies ++= Seq(
      "com.thesamet.test" % "test-protos" % "0.1" % "protobuf-src" intransitive(),
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion % "protobuf"
    ),

    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    )
  )

// Sub1 contains a proto file that imports a proto from test-protos.
// And another proto file from the externalProtos project.
lazy val sub1 = (project in file("sub1"))
  .dependsOn(externalProtos)
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    ),
  )
