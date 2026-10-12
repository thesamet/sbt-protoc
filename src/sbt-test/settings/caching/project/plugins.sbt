{
  val pluginVersion = System.getProperty("plugin.version")
  if (pluginVersion == null)
    throw new RuntimeException(
      """|The system property 'plugin.version' is not defined.
         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin
    )
  else addSbtPlugin("com.thesamet" % "sbt-protoc" % pluginVersion)
}

// Exercise older ScalaPB generators with the current protoc-bridge.
libraryDependencySchemes += "com.thesamet.scalapb" %% "protoc-bridge" % VersionScheme.Always

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.9"
