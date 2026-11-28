scalaVersion := "2.13.18"

Test / PB.targets := Seq(scalapb.gen() -> (Test / sourceManaged).value)

val checkClasspaths = taskKey[Unit]("Check that inferred ScalaPB runtime dependencies are test-only")
checkClasspaths := {
  val report = update.value
  val runtime = moduleFilter(organization = "com.thesamet.scalapb", name = "scalapb-runtime*")
  val compileRuntime = report.select(configurationFilter("compile"), runtime, artifactFilter())
  val testRuntime = report.select(configurationFilter("test"), runtime, artifactFilter())
  assert(compileRuntime.isEmpty, s"Runtime leaked into Compile: $compileRuntime")
  assert(testRuntime.nonEmpty, "Missing Test runtime")
}
