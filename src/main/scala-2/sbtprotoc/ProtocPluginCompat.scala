package sbtprotoc

import sbt.Configuration
import sbt.Classpaths
import sbt.Def
import sbt.UpdateReport
import sbt.Compile
import sbt.Test
import sjsonnew.BasicJsonProtocol.*
import sjsonnew.JsonFormat
import protocbridge.Artifact as BridgeArtifact
import xsbti.FileConverter
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport.platformDepsCrossVersion

trait ProtocPluginCompat { self: ProtocPlugin.type =>
  import autoImport.PB

  implicit val artifactFormat: JsonFormat[BridgeArtifact] =
    caseClassArray(BridgeArtifact.apply _, BridgeArtifact.unapply _)

  implicit val argumentsFormat: JsonFormat[Arguments] =
    caseClassArray(Arguments.apply _, Arguments.unapply _)

  implicit val UnpackedDependencyFormat: JsonFormat[UnpackedDependency] =
    caseClassArray(UnpackedDependency.apply _, UnpackedDependency.unapply _)

  implicit val UnpackedDependenciesFormat: JsonFormat[UnpackedDependencies] =
    caseClassArray(UnpackedDependencies.apply _, UnpackedDependencies.unapply _)

  def classpathsManagedJars(
      config: Configuration,
      jarTypes: Set[String],
      up: UpdateReport,
      converter: FileConverter
  ): Def.Classpath = Classpaths.managedJars(config, jarTypes, up)

  val additionalDependenciesValue = Def.setting {
    val compileLibs  = (Compile / PB.targets).value.flatMap(_.generator.suggestedDependencies)
    val testLibs     = (Test / PB.targets).value.flatMap(_.generator.suggestedDependencies)
    val crossVersion = platformDepsCrossVersion.?.value
    def dependencies(libs: Seq[BridgeArtifact]) = crossVersion match {
      case Some(c) =>
        libs.map { lib =>
          val a = makeArtifact(lib)
          if (lib.crossVersion)
            a cross c
          else
            a
        }
      case None =>
        libs.map(makeArtifact)
    }
    dependencies(compileLibs) ++ dependencies(testLibs).map(_ % Test)
  }
}
