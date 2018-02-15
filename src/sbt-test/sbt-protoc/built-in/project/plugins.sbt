{
  addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.16-SNAPSHOT")
}

libraryDependencies ++= Seq(
  "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.7"
)
