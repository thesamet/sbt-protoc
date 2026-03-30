import sbtprotoc.ProtocPlugin.ProtobufConfig

Compile / PB.targets := Seq(
  PB.gens.java -> (Compile / sourceManaged).value
)

Compile / PB.cacheStyle := PB.CacheStyle.ContentHash
Compile / PB.externalIncludePath := baseDirectory.value / "target" / "protobuf_external"

val depJar = settingKey[File]("Path to the local protobuf dependency jar")
depJar := baseDirectory.value / "deps" / "dep.jar"

ProtobufConfig / PB.unpackDependencies / managedClasspath := Seq(Attributed.blank(depJar.value))

val writeDepJar = inputKey[Unit]("Writes the protobuf dependency jar")
writeDepJar := {
  import complete.DefaultParsers._
  val version   = (Space ~> StringBasic).parsed
  val sourceDir = baseDirectory.value / "changes" / version
  val jar       = depJar.value
  IO.createDirectory(jar.getParentFile)
  IO.zip(Path.allSubpaths(sourceDir).toSeq, jar)
}

val protocCount = taskKey[Int]("Number of protoc invocations")
protocCount := ProtocCount.get()

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
