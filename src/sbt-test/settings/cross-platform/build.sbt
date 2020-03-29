import sbtcrossproject.crossProject

val checkDependency = taskKey[Unit]("")

lazy val crossPlatform = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .settings(
    scalaVersion := "2.11.12",
    PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value)
  )

val jvm = crossPlatform.jvm
  .settings(
    checkDependency := {
      val jarNames = (managedClasspath in Compile).value.map(_.data.getName)
      assert(jarNames.contains("scalapb-runtime_2.11-0.7.4.jar"), jarNames)
    }
  )

val js = crossPlatform.js
  .settings(
    checkDependency := {
      val jarNames = (managedClasspath in Compile).value.map(_.data.getName)
      assert(!jarNames.contains("scalapb-runtime_2.11-0.7.4.jar"), jarNames)
      assert(jarNames.contains("scalapb-runtime_sjs0.6_2.11-0.7.4.jar"), jarNames)
    }
  )

val native = crossPlatform.native
  .settings(
    checkDependency := {
      val jarNames = (managedClasspath in Compile).value.map(_.data.getName)
      assert(!jarNames.contains("scalapb-runtime_2.11-0.7.4.jar"), jarNames)
      assert(jarNames.contains("scalapb-runtime_native0.3_2.11-0.7.4.jar"), jarNames)
    }
  )
