addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject"      % "1.3.2")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.3.2")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"                   % "1.21.0")
addSbtPlugin("org.scala-native"   % "sbt-scala-native"              % "0.5.11")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.8")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.20"
