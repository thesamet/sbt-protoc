name := "sbt-protoc"

description := "SBT plugin for generating code from Protocol Buffer using protoc"

scalacOptions := Seq("-deprecation", "-unchecked")

scalacOptions ++= {
  scalaBinaryVersion.value match {
    case "2.12" =>
      Seq(
        "-release:8",
        "-Xlint",
        "-Yno-adapted-args"
      )
    case _ =>
      Nil
  }
}

def sbt2 = "2.0.0"

scalaVersion := "2.13.18"

crossScalaVersions += "3.8.4"

addSbtPlugin("com.github.sbt" % "sbt2-compat" % "0.1.0")

libraryDependencies ++= {
  val sbtV = (pluginCrossBuild / sbtBinaryVersion).value
  sbtV match {
    case "2" =>
      Seq(
        // https://github.com/scala/scala3/issues/18487
        "net.hamnaberg" %% "dataclass-annotation" % "0.3.0" % Provided
      )
    case _ =>
      val scalaV = (update / scalaBinaryVersion).value
      Seq(
        Defaults.sbtPluginExtra(
          "org.portable-scala" % "sbt-platform-deps" % "1.0.1",
          sbtV,
          scalaV
        )
      )
  }
}

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "protoc-bridge" % "0.9.10"
)

enablePlugins(SbtPlugin)

scriptedBufferLog := false

scriptedLaunchOpts += s"-Dplugin.version=${version.value}"

pluginCrossBuild / sbtVersion := {
  scalaBinaryVersion.value match {
    case "2.12" =>
      "1.9.9"
    case _ =>
      sbt2
  }
}

scriptedSbt := {
  scalaBinaryVersion.value match {
    case "2.12" =>
      sbtVersion.value
    case _ =>
      sbt2
  }
}

inThisBuild(
  List(
    organization := "com.thesamet",
    homepage     := Some(url("https://github.com/thesamet/sbt-protoc")),
    licenses     := List(
      "Apache-2.0" ->
        url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        "thesamet",
        "Nadav Samet",
        "thesamet@gmail.com",
        url("https://www.thesamet.com")
      )
    )
  )
)

TaskKey[Unit]("scriptedTestSbt2") := Def.taskDyn {
  val values = sbtTestDirectory.value
    .listFiles(_.isDirectory)
    .flatMap { dir1 =>
      dir1.listFiles(_.isDirectory).map { dir2 =>
        dir1.getName -> dir2.getName
      }
    }
    .toList
    .sorted
  val log                            = streams.value.log
  val exclude: Set[(String, String)] = Set(
    "compat"      -> "scalapb-0.10.3",
    "compat"      -> "scalapb-0.10.6",
    "compat"      -> "scalapb-0.9",
    "integration" -> "common-protos",
    "settings"    -> "caching",
    "settings"    -> "include-protos-in-jar",
    "settings"    -> "itconfig",
    "settings"    -> "non-jvm",
    "settings"    -> "protobuf-src",
    "settings"    -> "testconfig"
  )
  val args = values.filterNot(exclude).map { case (x1, x2) => s"${x1}/${x2}" }
  val arg  = args.mkString(" ", " ", "")
  log.info("scripted" + arg)
  scripted.toTask(arg)
}.value
