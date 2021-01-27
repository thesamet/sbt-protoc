Compile / PB.targets := Seq(
  (
    PB.gens.plugin("scala"),
    Seq()
  ) -> (Compile / sourceManaged).value,
  (
    PB.gens.plugin("scala"),
    Seq("flat_package")
  ) -> (Compile / sourceManaged).value
)

val scalapbcClassifier =
  if (protocbridge.SystemDetector.detectedClassifier().startsWith("windows")) "windows"
  else "unix"

libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime"  % "0.10.10"
libraryDependencies += "com.thesamet.scalapb"  % "protoc-gen-scala" % "0.10.10" % "protobuf" artifacts (
  Artifact(
    "protoc-gen-scala",
    PB.ProtocPlugin,
    "sh",
    scalapbcClassifier
  )
)
