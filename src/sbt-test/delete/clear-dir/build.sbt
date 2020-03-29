PB.targets in Compile := Seq(PB.gens.java -> (sourceManaged in Compile).value)
