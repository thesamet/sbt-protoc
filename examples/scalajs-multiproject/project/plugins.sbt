addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.19" exclude ("com.thesamet.scalapb", "protoc-bridge_2.10"))

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin-shaded" % "0.9.0-M5"

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.0")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.28")
