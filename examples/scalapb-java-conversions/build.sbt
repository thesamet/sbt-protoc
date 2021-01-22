import scalapb.compiler.Version.protobufVersion

Compile / PB.targets := Seq(
  PB.gens.java(protobufVersion) -> (Compile / sourceManaged).value,
  scalapb.gen(javaConversions = true) -> (Compile / sourceManaged).value
)

