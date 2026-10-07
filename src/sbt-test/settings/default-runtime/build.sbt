Compile / PB.targets := Seq(
  PB.gens.java   -> (Compile / sourceManaged).value,
  PB.gens.kotlin -> (Compile / sourceManaged).value
)

val checkDefaults = taskKey[Unit]("Check the default Java and Kotlin runtime versions")
checkDefaults := {
  val deps = PB.additionalDependencies.value.map(d => d.name -> d.revision).toMap
  assert(deps("protobuf-java") == PB.protocVersion.value, deps)
  assert(deps("protobuf-kotlin") == PB.protocVersion.value, deps)
}

val checkExplicit = taskKey[Unit]("Preserve explicitly selected runtime versions")
checkExplicit := {
  val deps = PB.additionalDependencies.value.map(d => d.name -> d.revision).toMap
  assert(deps("protobuf-java") == "3.24.4", deps)
  assert(deps("protobuf-kotlin") == "3.24.4", deps)
}
