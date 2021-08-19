Compile / PB.targets := Seq(
  PB.gens.descriptorSet -> (Compile / resourceManaged).value / "newdirectory" / "descriptorset.pb",
  PB.gens.js            -> (Compile / resourceManaged).value / "js",
  PB.gens.java          -> (Compile / sourceManaged).value
)

Compile / resourceGenerators += (Compile / PB.generate).taskValue
