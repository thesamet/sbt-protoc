{
  val pluginVersion = System.getProperty("plugin.version")
  if (pluginVersion == null)
    throw new RuntimeException(
      """|The system property 'plugin.version' is not defined.
         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin
    )
  else addSbtPlugin("com.thesamet" % "sbt-protoc" % pluginVersion)
}

libraryDependencySchemes +=  "com.thesamet.scalapb" %% "protoc-bridge" % VersionScheme.Always

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "compilerplugin" % "0.9.8"
)
