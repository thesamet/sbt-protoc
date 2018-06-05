{
  val pluginVersion = System.getProperty("plugin.version")
  if(pluginVersion == null)
    throw new RuntimeException("""|The system property 'plugin.version' is not defined.
                                  |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
  else addSbtPlugin("com.thesamet" % "sbt-protoc" % pluginVersion)
}

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.4.0")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "0.4.0")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.3.7")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.23")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.7.4"
