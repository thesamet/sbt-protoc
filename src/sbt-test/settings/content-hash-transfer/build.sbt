import sbtcompat.PluginCompat._

Compile / PB.cacheStyle := {
  if ((baseDirectory.value / "mtime-mode").exists) PB.CacheStyle.LastModified
  else PB.CacheStyle.ContentHash
}
Compile / PB.targets := Seq(PB.gens.java -> (baseDirectory.value / "generated"))

Compile / PB.runProtoc := Def.uncached {
  val original = (Compile / PB.runProtoc).value
  val countFile = baseDirectory.value / "invocations"
  (args, extraEnv) => {
    val count = if (countFile.exists) IO.read(countFile).trim.toInt else 0
    IO.write(countFile, (count + 1).toString)
    original.run(args, extraEnv)
  }
}

val assertProtocCount = inputKey[Unit]("Check invocation count across sbt restarts")
assertProtocCount := {
  import complete.DefaultParsers._
  val expected = (Space ~> IntBasic).parsed
  val actual = IO.read(baseDirectory.value / "invocations").trim.toInt
  assert(actual == expected, s"Expected $expected protoc invocations, got $actual")
}

val restoreArtifacts = taskKey[Unit]("Round-trip cached artifacts through zip, resetting timestamps")
restoreArtifacts := Def.uncached {
  val root = baseDirectory.value
  // Only protobuf caches and generated outputs are transferred; paths are preserved.
  val cache = (Compile / PB.generate / streams).value.cacheDirectory
  val generated = root / "generated"
  val archive = root / "artifacts.zip"
  val files = (cache.allPaths.get() ++ generated.allPaths.get()).filter(_.isFile)
  assert(files.nonEmpty, "No artifacts to transfer")
  IO.zip(files.map(f => f -> IO.relativize(root, f).get), archive, None)
  IO.delete(cache)
  IO.delete(generated)
  IO.unzip(archive, root)
  val inputs = (root / "src").allPaths.get().filter(_.isFile)
  val timestamp = java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() + 10000L)
  (files ++ inputs).foreach(f => java.nio.file.Files.setLastModifiedTime(f.toPath, timestamp))
}
