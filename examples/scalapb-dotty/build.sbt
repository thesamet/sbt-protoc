scalaVersion := "3.0.0-M1"

PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value)
