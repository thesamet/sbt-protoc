import protocbridge.{SandboxedJvmGenerator, Artifact}

scalaVersion := "2.13.18"

val scalaGen = Def.setting {
  val v = sbtBinaryVersion.value match {
    case "1.0" => "2.12"
    case "2" => "3"
  }
  val scalapbVersion = "1.0.0-alpha.5"
  SandboxedJvmGenerator.forModule(
    "scala",
    Artifact(
      "com.thesamet.scalapb",
      s"compilerplugin_${v}",
      scalapbVersion
    ),
    "scalapb.ScalaPbCodeGenerator$",
    suggestedDependencies = Seq(
      Artifact("com.thesamet.scalapb", "scalapb-runtime", scalapbVersion, true)
    )
  )
}

Compile / PB.targets := Seq(scalaGen.value -> (Compile / sourceManaged).value)

libraryDependencies += "com.google.protobuf" % "protobuf-java" % "3.8.0" % "protobuf"
