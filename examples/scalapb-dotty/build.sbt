scalaVersion := "3.0.0-M1"

Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value / "scalapb")
