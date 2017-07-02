addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.10" exclude ("com.trueaccord.scalapb", "protoc-bridge_2.10"))

libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin-shaded" % "0.6.0-pre5"

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.15")
