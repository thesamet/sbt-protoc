Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value)
