scalaVersion := "2.13.1"

PB.targets in Compile := Seq(PB.gens.java -> (sourceManaged in Compile).value)
