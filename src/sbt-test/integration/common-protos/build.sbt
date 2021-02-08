ThisBuild / scalaVersion := "2.13.4"

lazy val top = (project in file("top"))
  .settings(commonSettings)

lazy val bottom = (project in file("bottom"))
  .dependsOn(top)
  .settings(commonSettings)

lazy val commonSettings = Seq(
  Compile / PB.targets := Seq(scalapb.gen(flatPackage = true) -> (Compile / sourceManaged).value),
  libraryDependencies ++= Seq(
    // Announcing compilation with flatPackage=false in `ScalaPB-Options-Proto`
    "com.thesamet.scalapb.common-protos" %% "pgv-proto-scalapb_0.10" % "0.4.1-1" % "protobuf",
    "com.thesamet.scalapb.common-protos" %% "pgv-proto-scalapb_0.10" % "0.4.1-1"
  )
)
