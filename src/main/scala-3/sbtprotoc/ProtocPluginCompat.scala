package sbtprotoc

import sbt.*
import java.io.File
import sjsonnew.BasicJsonProtocol.caseClassArray
import sjsonnew.BasicJsonProtocol.given
import sjsonnew.JsonFormat
import sjsonnew.IsoString
import protocbridge.{Artifact => BridgeArtifact}

trait ProtocPluginCompat { self: ProtocPlugin.type =>
  import autoImport.PB

  given JsonFormat[BridgeArtifact] =
    caseClassArray(
      BridgeArtifact.apply,
      (x: BridgeArtifact) => Option(Tuple.fromProductTyped(x))
    )

  given JsonFormat[Arguments] =
    caseClassArray(
      Arguments.apply,
      (x: Arguments) => Option(Tuple.fromProductTyped(x))
    )

  given JsonFormat[UnpackedDependency] = {
    caseClassArray(
      UnpackedDependency.apply,
      (x: UnpackedDependency) => Option(Tuple.fromProductTyped(x))
    )
  }

  given JsonFormat[UnpackedDependencies] = {
    given IsoString[File] = {
      val iso = summon[sjsonnew.IsoStringLong[File]]
      IsoString.iso(
        (file: File) => iso.to(file)._1,
        (s: String) => iso.from((s, 0))
      )
    }

    caseClassArray(
      UnpackedDependencies.apply,
      (x: UnpackedDependencies) => Option(x.mappedFiles)
    )
  }

  def classpathsManagedJars(
      config: Configuration,
      jarTypes: Set[String],
      up: UpdateReport,
      converter: FileConverter
  ): Def.Classpath =
    Classpaths.managedJars(config, jarTypes, up, converter)

  val additionalDependenciesValue = Def.setting {
    val libs = (Compile / PB.targets).value.flatMap(_.generator.suggestedDependencies)
    libs.map(makeArtifact)
  }
}
