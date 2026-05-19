val protobufVersion = "3.11.4"

lazy val root = (project in file("."))
  .dependsOn(a)
  .settings(commonSettings: _*)
  .settings(
    InputKey[Unit]("check") := {
      val files = sbtBinaryVersion.value match {
        case "1.0" =>
          Seq(
            "target/scala-2.12/src_managed/main/multi/File1.java",
            "target/scala-2.12/classes/multi/File1.class",
            "sub-a/target/scala-2.12/src_managed/main/sub/a/A.java",
            "sub-a/target/scala-2.12/classes/sub/a/A.class",
            "sub-b/target/scala-2.12/src_managed/main/sub/b/B.java",
            "sub-b/target/scala-2.12/classes/sub/b/B.class",
          )
        case "2" =>
          Seq(
            "target/out/jvm/scala-2.12.21/a/src_managed/main/sub/a/A.java",
            "target/out/jvm/scala-2.12.21/a/classes/sub/a/A.class",
            "target/out/jvm/scala-2.12.21/root/src_managed/main/multi/File1.java",
            "target/out/jvm/scala-2.12.21/root/classes/multi/File1.class",
            "target/out/jvm/scala-2.12.21/b/src_managed/main/sub/b/B.java",
            "target/out/jvm/scala-2.12.21/b/classes/sub/b/B.class",
          )
      }
      files.foreach(f => assert(file(f).isFile, f))
    },
  )

lazy val a = (project in file("sub-a"))
  .dependsOn(b)
  .settings(commonSettings: _*)

lazy val b = (project in file("sub-b"))
  .settings(commonSettings: _*)

lazy val commonSettings = Seq[SettingsDefinition](
  scalaVersion := "2.12.21",
  Compile / PB.targets := Seq(PB.gens.java -> (Compile / sourceManaged).value),
  Test / PB.targets    := Seq(PB.gens.java -> (Test / sourceManaged).value),
  inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings),
  libraryDependencies += "com.google.protobuf" % "protobuf-java" % protobufVersion % "protobuf"
)
