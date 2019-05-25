import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

lazy val proto = (crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure) in file("proto"))
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    ),
    // The trick is in this line:
    PB.protoSources in Compile := Seq(file("proto/src/main/protobuf")),
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion,
      "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
    )
  )

lazy val protoJs = proto.js
lazy val protoJVM = proto.jvm

lazy val client = (project in file("client"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "client"
  )
  .dependsOn(protoJs)

lazy val server = (project in file("server"))
  .settings(
    name := "server"
  )
  .dependsOn(protoJVM)
