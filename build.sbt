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

def sbt2 = "2.0.0-RC13"

scalaVersion := "2.12.20"

crossScalaVersions += "3.8.3"

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
  "com.thesamet.scalapb" %% "protoc-bridge" % "0.9.9"
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

sonatypeProfileName := "com.thesamet"

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
