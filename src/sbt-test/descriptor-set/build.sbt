PB.targets in Compile := Seq(
  PB.gens.descriptorSet -> file("newdirectory/descriptorset.pb"),
  PB.gens.java -> (sourceManaged in Compile).value // just make sure descriptorSet is compatible with other generators
)