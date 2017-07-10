package sbtprotoc

import java.io.File

import sbt.librarymanagement.{CrossVersion, ModuleID}
import sbt.util.CacheImplicits
import sjsonnew.JsonFormat

private[sbtprotoc] trait Compat extends CacheImplicits { self: ProtocPlugin.type =>
  private val CrossDisabled = sbt.librarymanagement.Disabled()
  protected def makeArtifact(f: protocbridge.Artifact): ModuleID = {
    ModuleID(f.groupId, f.artifactId, f.version)
      .cross(if (f.crossVersion) CrossVersion.binary else CrossDisabled)
  }

  protected object CacheArguments {
    implicit val instance: JsonFormat[Arguments] = project(
      (a: Arguments) => (a.includePaths, a.protocOptions, a.pythonExe, a.deleteTargetDirectory, a.targets),
      (in: (Seq[File], Seq[String], String, Boolean, Seq[(File, Seq[String])])) => Arguments(in._1, in._2, in._3, in._4, in._5)
    )
  }
}
