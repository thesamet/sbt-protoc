import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import scalapb.compiler.Version.scalapbVersion

val Scala212 = "2.12.19"
val Scala213 = "2.13.12"

val sharedSettings = Seq(
  name                      := "example",
  version                   := "0.1.0",
  scalaVersion              := "2.13.12",
  Compile / PB.protoSources := Seq((ThisBuild / baseDirectory).value / "src" / "main" / "protobuf"),
  Compile / PB.targets := Seq(
    scalapb.gen() -> (Compile / sourceManaged).value / "protos"
  ),
  libraryDependencies += "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapbVersion,
  scalaJSUseMainModuleInitializer                := true,
  buildInfoKeys    := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
  buildInfoPackage := "hello"
)

lazy val example = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(sharedSettings)
  .jsSettings(crossScalaVersions := Seq(Scala212, Scala213))
  .jvmSettings(crossScalaVersions := Seq(Scala212, Scala213))
  .nativeSettings(crossScalaVersions := Seq("2.12.19", "2.13.12"))

lazy val exampleJS     = example.js
lazy val exampleJVM    = example.jvm
lazy val exampleNative = example.native

// The following is needed since otherwise SBT would create a default root project rooted in `.`
// It will try compiling sources under `src/main/scala`, but without access to the generated code.
// To prevent that, we explictly set the source directory to something inexistent...
lazy val root = project
  .in(file("."))
  .aggregate(exampleJS, exampleJVM, exampleNative)
  .settings(
    sourceDirectory := file("ignore"),
    publish         := {}
  )
