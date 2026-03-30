import sbtprotoc.ProtocPlugin.ProtobufConfig

Compile / PB.targets := Seq(
  PB.gens.java -> (Compile / sourceManaged).value
)

Compile / PB.cacheStyle := PB.CacheStyle.ContentHash
Compile / PB.externalIncludePath := baseDirectory.value / "target" / "protobuf_external"

val unpackHookFile = new File("target/unpack-hook.log").getAbsoluteFile
val _ = {
  IO.delete(unpackHookFile)
  System.setProperty("sbtprotoc.test.unpackHookFile", unpackHookFile.getAbsolutePath)
}

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

val unpackCount = taskKey[Int]("Number of actual unpack executions")
unpackCount := {
  if (unpackHookFile.exists()) IO.readLines(unpackHookFile).count(_.nonEmpty)
  else 0
}

Compile / PB.runProtoc := {
  val original = (Compile / PB.runProtoc).value
  (args, extraEnv) => {
    ProtocCount.incrementAndGet()
    original.run(args, extraEnv)
  }
}

val assertUnpackCount = inputKey[Unit]("Assert unpack execution count")
assertUnpackCount := {
  import complete.DefaultParsers._
  val expected = (Space ~> IntBasic).parsed
  val actual   = unpackCount.value
  assert(actual == expected, s"Expected unpack count $expected but got $actual")
}

val assertProtocCount = inputKey[Unit]("Assert protoc invocation count")
assertProtocCount := {
  import complete.DefaultParsers._
  val expected = (Space ~> IntBasic).parsed
  val actual   = ProtocCount.get()
  assert(actual == expected, s"Expected protoc count $expected but got $actual")
}

val assertFooReferencesDep = taskKey[Unit]("Assert generated Foo references dep.Dep")
assertFooReferencesDep := {
  val generated = (Compile / sourceManaged).value / "mypkg" / "FooOuterClass.java"
  val contents  = IO.read(generated)
  assert(contents.contains("dep.Dep"), s"Expected $generated to reference dep.Dep")
}
