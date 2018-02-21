addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.15" exclude ("com.thesamet.scalapb", "protoc-bridge_2.10"))

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin-shaded" % "0.7.0"

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.21")
