libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.4")

addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.7")
