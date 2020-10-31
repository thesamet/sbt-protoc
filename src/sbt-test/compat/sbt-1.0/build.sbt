scalaVersion := "2.12.12"

PB.targets in Compile := Seq(PB.gens.java -> (sourceManaged in Compile).value)
