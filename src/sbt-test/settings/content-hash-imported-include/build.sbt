Compile / PB.targets := Seq(
  PB.gens.java -> (Compile / sourceManaged).value
)

Compile / PB.cacheStyle := PB.CacheStyle.ContentHash
Compile / PB.includePaths += baseDirectory.value / "external"

Compile / PB.runProtoc := {
  val original = (Compile / PB.runProtoc).value
  (args, extraEnv) => {
    ProtocCount.incrementAndGet()
    original.run(args, extraEnv)
  }
}

val assertProtocCount = inputKey[Unit]("Assert protoc invocation count")
assertProtocCount := {
  import complete.DefaultParsers._
  val expected = (Space ~> IntBasic).parsed
  val actual   = ProtocCount.get()
  assert(actual == expected, s"Expected protoc count $expected but got $actual")
}
