import sbtcompat.PluginCompat._

Compile / PB.targets := Seq(
  PB.gens.java -> (baseDirectory.value / "generated")
)

Compile / PB.cacheStyle := PB.CacheStyle.ContentHash

Compile / PB.runProtoc := Def.uncached {
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

// A timestamp-only stamp would miss this edit.
val changeWithSameTimestamp = taskKey[Unit]("Change a proto without changing its timestamp")
changeWithSameTimestamp := Def.uncached {
  val dest = baseDirectory.value / "src/main/protobuf/foo.proto"
  val time = java.nio.file.Files.getLastModifiedTime(dest.toPath)
  IO.copyFile(baseDirectory.value / "changes/foo-v2.proto", dest)
  java.nio.file.Files.setLastModifiedTime(dest.toPath, time)
  assert(java.nio.file.Files.getLastModifiedTime(dest.toPath) == time)
}

val assertFullRuntime = taskKey[Unit]("Verify outputs match the current generator options")
assertFullRuntime := Def.uncached {
  val contents = IO.read(baseDirectory.value / "generated/mypkg/FooOuterClass.java")
  assert(contents.contains("com.google.protobuf.GeneratedMessageV3"), "Stale lite output")
}
