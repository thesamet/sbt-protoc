import sbtprotoc.ProtocPlugin.ProtobufSrcConfig

Compile / PB.targets := Seq(
  PB.gens.java -> (Compile / sourceManaged).value
)

Compile / PB.cacheStyle         := PB.CacheStyle.ContentHash
Compile / PB.externalSourcePath := baseDirectory.value / "target" / "protobuf_external_src"

val depJar = settingKey[File]("Path to the local protobuf-src dependency jar")
depJar := baseDirectory.value / "deps" / "dep-src.jar"

ProtobufSrcConfig / PB.unpackDependencies / managedClasspath := Seq(Attributed.blank(depJar.value))

val writeDepJar = inputKey[Unit]("Writes the protobuf-src dependency jar")
writeDepJar := {
  import complete.DefaultParsers._
  val version   = (Space ~> StringBasic).parsed
  val sourceDir = baseDirectory.value / "changes" / version
  val jar       = depJar.value
  IO.createDirectory(jar.getParentFile)
  IO.zip(Path.allSubpaths(sourceDir).toSeq, jar)
}

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

val assertDepSourceGenerated = taskKey[Unit]("Assert protobuf-src dependency generated its own source")
assertDepSourceGenerated := {
  val generated = (Compile / sourceManaged).value / "dep" / "DepOuterClass.java"
  assert(generated.exists(), s"Expected generated source for protobuf-src dependency at $generated")
}

// The extracted dep.proto content is the direct observable of an unpack cache
// miss: v1 defines only `string name`, v2 adds `int32 age`. Checking the
// extracted file distinguishes the two versions without production-side
// instrumentation.
// "int32 age" is the v2-specific signature. Matching just "age" would yield
// false positives because `package dep;` contains "age" as a substring.
val assertExtractedDepIsV1 = taskKey[Unit]("Extracted dep.proto must match v1 (no age field)")
assertExtractedDepIsV1 := {
  val extracted = (Compile / PB.externalSourcePath).value / "dep" / "dep.proto"
  assert(extracted.exists(), s"Expected extracted file at $extracted")
  val content = IO.read(extracted)
  assert(
    !content.contains("int32 age"),
    s"Expected v1 (no age field) but $extracted contains:\n$content"
  )
}

val assertExtractedDepIsV2 = taskKey[Unit]("Extracted dep.proto must match v2 (has age field)")
assertExtractedDepIsV2 := {
  val extracted = (Compile / PB.externalSourcePath).value / "dep" / "dep.proto"
  assert(extracted.exists(), s"Expected extracted file at $extracted")
  val content = IO.read(extracted)
  assert(
    content.contains("int32 age"),
    s"Expected v2 (age field) but $extracted contains:\n$content"
  )
}
