lazy val pbSettings = Seq(
  PB.targets in Compile := Seq(
    scalapb.gen() -> (sourceManaged in Compile).value
  ),
  libraryDependencies += "com.google.protobuf" % "protobuf-java" % "3.0.0" % "protobuf",
  unmanagedResourceDirectories in Compile += (baseDirectory in Compile).value / "src/main/protobuf"
)

lazy val root = project.in(file("."))
  .aggregate(moduleA, moduleB)

lazy val moduleA = project.in(file("module_a"))
  .settings(
    pbSettings: _*
  )

lazy val moduleB = project.in(file("module_b"))
  .dependsOn(moduleA)
  .settings(
    pbSettings: _*
  )
