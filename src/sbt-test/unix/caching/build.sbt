val setReadable = inputKey[Unit]("")
setReadable := {
  import complete.DefaultParsers._
  val (file, readable) = (fileParser((ThisBuild / baseDirectory).value) ~ (Space ~> Bool)).parsed
  file.setReadable(readable)
}

Compile / PB.targets := Seq(
  PB.gens.java  -> (Compile / sourceManaged).value,
  scalapb.gen() -> (Compile / sourceManaged).value
)
PB.additionalDependencies := Seq("com.google.protobuf" % "protobuf-java" % "3.13.0" % "protobuf")
