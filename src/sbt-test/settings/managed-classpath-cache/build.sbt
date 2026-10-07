import sbt.librarymanagement.{ConfigRef, ConfigurationReport, ModuleReport, UpdateReport, UpdateStats}
import sbtcompat.PluginCompat.{toFile => compatFile, _}
import sbtprotoc.ProtocPlugin.{ProtobufConfig, ProtobufSrcConfig}

val fixtureReport    = settingKey[UpdateReport]("Report with an unrelated compile dependency")
val selectClasspaths = taskKey[Unit]("Check the plugin selects the correct dependencies")
val resetProbe       = taskKey[Unit]("Reset unrelated artifact access count after warming the cache")
val checkClasspaths = taskKey[Unit]("Select protobuf dependencies without reading unrelated artifacts")

fixtureReport := {
  val root = baseDirectory.value
  def configReport(name: String, artifactName: String) = {
    val artifactFile = root / s"$artifactName.jar"
    ConfigurationReport(
      ConfigRef(name),
      Vector(
        ModuleReport(
          "test" % artifactName % "1.0",
          Vector(
            Artifact(artifactName) ->
              (if (name == "compile") HashProbe.file(artifactFile) else artifactFile)
          ),
          Vector.empty
        )
      ),
      Vector.empty
    )
  }
  UpdateReport(
    root / "descriptor.xml",
    Vector(
      configReport("compile", "unrelated"),
      configReport(ProtobufConfig.name, "include"),
      configReport(ProtobufSrcConfig.name, "source")
    ),
    UpdateStats(0L, 0L, 0L, true),
    Map.empty
  )
}

// Supply the same report on every invocation without resolution or an upstream cache
// reading the fixture files. The managedClasspath definitions are the plugin's own.
ProtobufConfig / update    := Def.uncached(fixtureReport.value)
ProtobufSrcConfig / update := Def.uncached(fixtureReport.value)

selectClasspaths := Def.uncached {
  implicit val converter: xsbti.FileConverter = fileConverter.value
  val includes = (ProtobufConfig / managedClasspath).value.map(a => compatFile(a.data).getName)
  val sources  = (ProtobufSrcConfig / managedClasspath).value.map(a => compatFile(a.data).getName)
  assert(includes == Seq("include.jar"), s"Unexpected include classpath: $includes")
  assert(sources == Seq("source.jar"), s"Unexpected source classpath: $sources")
}

resetProbe := Def.uncached { HashProbe.accesses.set(0) }

checkClasspaths := Def.uncached {
  selectClasspaths.value
  val accesses = HashProbe.accesses.get()
  assert(
    accesses == 0,
    s"managedClasspath accessed an unrelated artifact $accesses times on a warm run"
  )
}
