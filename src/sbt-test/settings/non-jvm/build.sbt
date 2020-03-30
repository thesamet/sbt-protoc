PB.targets in Compile := Seq(
  PB.gens.descriptorSet -> (resourceManaged in Compile).value / "newdirectory" / "descriptorset.pb",
  PB.gens.js -> (resourceManaged in Compile).value / "js",
  PB.gens.java -> (sourceManaged in Compile).value
)

Compile / resourceGenerators += (Compile / PB.generate).taskValue