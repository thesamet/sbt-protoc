package sbtprotoc

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
    implicit val instance: JsonFormat[Arguments] =
      caseClassArray(Arguments.apply _, Arguments.unapply _)
  }
}
