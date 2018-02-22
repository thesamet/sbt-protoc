lazy val x = project.disablePlugins(ProtocPlugin)

lazy val y = project.dependsOn(x)
