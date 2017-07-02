import com.trueaccord.scalapb.compiler.Version.protobufVersion

PB.targets in Compile := Seq(
  PB.gens.java(protobufVersion) -> (sourceManaged in Compile).value,
  scalapb.gen(javaConversions = true) -> (sourceManaged in Compile).value
)

