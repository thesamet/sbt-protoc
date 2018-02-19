package sbtprotoc

private[sbtprotoc] trait Compat { self: ProtocPlugin.type =>
  import sbt.ModuleID
  import sbt.CrossVersion
  protected def makeArtifact(f: protocbridge.Artifact): ModuleID = {
    ModuleID(f.groupId, f.artifactId, f.version, crossVersion =
      if (f.crossVersion) CrossVersion.binary else CrossVersion.Disabled)
  }

  protected object CacheArguments {
    import sbt.Cache.seqFormat
    import sbinary.DefaultProtocol._
    implicit val instance: sbinary.Format[Arguments] =
      asProduct4(Arguments.apply)(Function.unlift(Arguments.unapply))
  }
}
