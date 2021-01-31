import scalapb.compiler.Version.protobufVersion

scalaVersion := "2.13.4"

Compile / PB.targets := Seq(scalapb.gen(flatPackage=true) -> (Compile / sourceManaged).value)

libraryDependencies ++= Seq(
  "com.thesamet.scalapb.common-protos" %% "pgv-proto-scalapb_0.10" % "0.4.1-1" % "protobuf",
  "com.thesamet.scalapb.common-protos" %% "pgv-proto-scalapb_0.10" % "0.4.1-1"
)
