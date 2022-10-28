Compile / PB.targets := Seq(
  PB.gens.descriptorSet -> (Compile / resourceManaged).value / "newdirectory" / "descriptorset.pb",
  PB.gens.ruby          -> (Compile / resourceManaged).value / "ruby",
  PB.gens.java          -> (Compile / sourceManaged).value
)

Compile / resourceGenerators += (Compile / PB.generate).taskValue
