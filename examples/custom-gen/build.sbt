PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value,
  MyCodeGenerator -> (sourceManaged in Compile).value
)

