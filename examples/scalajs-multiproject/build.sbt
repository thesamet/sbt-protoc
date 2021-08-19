import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

ThisBuild / scalaVersion := "2.13.3"

lazy val proto = (crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure) in file("proto"))
  .settings(
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value
    ),
    // The trick is in this line:
    Compile / PB.protoSources := Seq(file("proto/src/main/protobuf")),
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion,
      "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
    )
  )

lazy val protoJs  = proto.js
lazy val protoJVM = proto.jvm

lazy val client = (project in file("client"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name                            := "client",
    scalaJSUseMainModuleInitializer := true
  )
  .dependsOn(protoJs)

lazy val server = (project in file("server"))
  .settings(
    name := "server"
  )
  .dependsOn(protoJVM)
