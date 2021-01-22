Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value,
  MyCodeGenerator -> (Compile / sourceManaged).value
)

