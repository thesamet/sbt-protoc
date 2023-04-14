// PB.targets, no proto
lazy val a = project
  .settings(
    PB.protocVersion     := "1.2.3+non-existing-a",
    Compile / PB.targets := Seq(PB.gens.java -> (Compile / sourceManaged).value)
  )

// no PB.targets, one proto
lazy val b = project
  .settings(
    PB.protocVersion := "1.2.3+non-existing-b"
  )

// PB.targets, one ignored proto
lazy val c = project
  .settings(
    PB.protocVersion            := "1.2.3+non-existing-c",
    Compile / PB.targets        := Seq(PB.gens.java -> (Compile / sourceManaged).value),
    PB.generate / excludeFilter := "ignored.proto"
  )
