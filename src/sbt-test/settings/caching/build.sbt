import java.nio.file.Files
import java.nio.file.attribute._
import scala.jdk.CollectionConverters._
import protocbridge.{SandboxedJvmGenerator, Target}

/** Update the attributes of the file passed as an argument to make it readable or not for the current user,
  * without updating the "last modified" attribute nor changing the content of the file.
  *
  * Preventing read access to files that are input of protoc is a cheap way to check whether protoc is
  * actually called (as long as there is no prior content hashing done by sbt-protoc) by asserting
  * on the failure of the next run.
  */
val setReadable = inputKey[Unit]("")
setReadable := {
  import complete.DefaultParsers._
  val (file, readable) = (fileParser((ThisBuild / baseDirectory).value) ~ (Space ~> Bool)).parsed
  if (protocbridge.SystemDetector.detectedClassifier().startsWith("windows")) {
    val view = Files.getFileAttributeView(file.toPath, classOf[AclFileAttributeView])
    val updatedAcls = view.getAcl.asScala.map { acl =>
      val permissions = acl.permissions
      if (readable) permissions.add(AclEntryPermission.READ_DATA)
      else permissions.remove(AclEntryPermission.READ_DATA)
      AclEntry.newBuilder(acl).setPermissions(permissions).build
    }
    view.setAcl(updatedAcls.asJava)
  } else file.setReadable(readable)
}

val DummyArtifact = protocbridge.Artifact("DummyGroup", "DummyArtifact", "DummyVersion")
val localGen      = SandboxedJvmGenerator.forModule("LocalGen", DummyArtifact, "codegen.LocalGen$", Nil)

lazy val api = (project in file("api"))
  .settings(
    Compile / PB.targets := Seq(
      PB.gens.java -> (Compile / sourceManaged).value,
      Target(PB.gens.plugin("validate"), (Compile / sourceManaged).value, Seq("lang=java")),
      scalapb.gen() -> (Compile / sourceManaged).value,
      localGen      -> (Compile / sourceManaged).value,
      localGen      -> (Compile / resourceManaged).value // use 2 generators with the same artifact to check dedup
    ),
    PB.additionalDependencies ++= Seq(
      "com.google.protobuf"                % "protobuf-java"       % "3.13.0" % "protobuf",
      ("io.envoyproxy.protoc-gen-validate" % "protoc-gen-validate" % "0.4.0").asProtocPlugin
    ),
    PB.artifactResolver := {
      val oldResolver = PB.artifactResolver.value
      val cp          = (codegen / Compile / fullClasspath).value.map(_.data)
      (a: protocbridge.Artifact) =>
        a match {
          case DummyArtifact =>
            // every other resolution triggers failure in the next protocGenerate
            if (Count.incrementAndGet % 2 != 0) cp else Nil
          case other => oldResolver(other)
        }
    }
  )

lazy val codegen = (project in file("codegen"))
  .settings(
    scalaVersion := scala.util.Properties.versionNumberString,
    libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % scalapb.compiler.Version.scalapbVersion
  )
