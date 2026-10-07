{
  val pluginVersion = System.getProperty("plugin.version")
  if (pluginVersion == null)
    throw new RuntimeException(
      """|The system property 'plugin.version' is not defined.
         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin
    )
  else addSbtPlugin("com.thesamet" % "sbt-protoc" % pluginVersion)
}

// To reproduce the failure before ScalaPB#2228:
// SCALAPB_TEST_VERSION=0.11.20 sbt "++ 3.x" "scripted compat/scalapb-0.11"
// https://github.com/scalapb/ScalaPB/pull/2228
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" %
  sys.env.getOrElse("SCALAPB_TEST_VERSION", "0.11.21")
