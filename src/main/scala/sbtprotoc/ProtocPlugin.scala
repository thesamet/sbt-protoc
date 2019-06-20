package sbtprotoc

import sbt._
import Keys._
import java.io.File

import protocbridge.Target
import sbt.plugins.JvmPlugin
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport.platformDepsCrossVersion
import java.{util => ju}
import java.nio.file.attribute.PosixFilePermission

object ProtocPlugin extends AutoPlugin with Compat {
  object autoImport {
    object PB {
      val includePaths = SettingKey[Seq[File]](
        "protoc-include-paths",
        "The paths that contain *.proto dependencies."
      )
      val additionalDependencies = SettingKey[Seq[ModuleID]](
        "additional-dependencies",
        "Additional dependencies to be added to library dependencies."
      )
      val externalIncludePath = SettingKey[File](
        "protoc-external-include-path",
        "The path to which protobuf:libraryDependencies are extracted and which is used as protobuf:includePath for protoc"
      )
      val dependentProjectsIncludePaths = SettingKey[Seq[File]](
        "protoc-dependent-projects-include-paths",
        "The paths to the protoc files of projects being depended on."
      )
      val generate = TaskKey[Seq[File]]("protoc-generate", "Compile the protobuf sources.")
      val unpackDependencies =
        TaskKey[UnpackedDependencies]("protoc-unpack-dependencies", "Unpack dependencies.")

      val protocOptions =
        SettingKey[Seq[String]]("protoc-options", "Additional options to be passed to protoc")
      val protoSources =
        SettingKey[Seq[File]]("protoc-sources", "Directories to look for source files")
      val targets = SettingKey[Seq[Target]]("protoc-targets", "List of targets to generate")

      val runProtoc = SettingKey[Seq[String] => Int](
        "protoc-run-protoc",
        "A function that executes the protobuf compiler with the given arguments, returning the exit code of the compilation run."
      )
      val protocVersion = SettingKey[String]("protoc-version", "Version flag to pass to protoc-jar")

      val pythonExe = SettingKey[String](
        "python-executable",
        "Full path for a Python.exe (deprecated and ignored)"
      )

      val deleteTargetDirectory = SettingKey[Boolean](
        "delete-target-directory",
        "Delete target directory before regenerating sources."
      )
      val recompile = TaskKey[Boolean]("protoc-recompile")

      val Target       = protocbridge.Target
      val gens         = protocbridge.gens
      val ProtocPlugin = "protoc-plugin"
    }

    implicit class AsProtocPlugin(val moduleId: ModuleID) extends AnyVal {
      def asProtocPlugin: ModuleID = {
        moduleId % "protobuf" artifacts (Artifact(
          name = moduleId.name,
          `type` = PB.ProtocPlugin,
          extension = "exe",
          classifier = SystemDetector.detectedClassifier()
        ))
      }
    }
  }

  // internal key for detect change options
  private[this] val arguments = TaskKey[Arguments]("protoc-arguments")

  private[sbtprotoc] final case class Arguments(
      includePaths: Seq[File],
      protocOptions: Seq[String],
      deleteTargetDirectory: Boolean,
      targets: Seq[(File, Seq[String])]
  )

  import autoImport.PB

  val ProtobufConfig = config("protobuf")

  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = JvmPlugin

  override def projectConfigurations: Seq[Configuration] = Seq(ProtobufConfig)

  def protobufGlobalSettings: Seq[Def.Setting[_]] = Seq(
    includeFilter in PB.generate := "*.proto",
    PB.externalIncludePath := target.value / "protobuf_external",
    PB.unpackDependencies := unpackDependenciesTask(PB.unpackDependencies).value,
    PB.additionalDependencies := {
      val libs = (PB.targets in Compile).value.flatMap(_.generator.suggestedDependencies)
      platformDepsCrossVersion.?.value match {
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
    },
    libraryDependencies ++= PB.additionalDependencies.value,
    classpathTypes in ProtobufConfig += PB.ProtocPlugin,
    managedClasspath in ProtobufConfig := {
      val artifactTypes: Set[String] = (classpathTypes in ProtobufConfig).value
      Classpaths.managedJars(ProtobufConfig, artifactTypes, (update in ProtobufConfig).value)
    },
    ivyConfigurations += ProtobufConfig,
    PB.protocVersion := "-v371",
    PB.pythonExe := "python",
    PB.deleteTargetDirectory := true
  )

  // Settings that are applied at configuration (Compile, Test) scope.
  def protobufConfigSettings: Seq[Setting[_]] = Seq(
    arguments := Arguments(
      includePaths = PB.includePaths.value,
      protocOptions = PB.protocOptions.value,
      deleteTargetDirectory = PB.deleteTargetDirectory.value,
      targets = PB.targets.value.map(target => (target.outputPath, target.options))
    ),
    PB.recompile := {
      import CacheArguments.instance
      arguments.previous.exists(_ != arguments.value)
    },
    PB.protocOptions := Nil,
    PB.protocOptions := PB.protocOptions.?.value.getOrElse(Nil),
    PB.protoSources := PB.protoSources.?.value.getOrElse(Nil),
    PB.protoSources += sourceDirectory.value / "protobuf",
    PB.includePaths := PB.includePaths.?.value.getOrElse(Nil),
    PB.includePaths ++= PB.protoSources.value,
    PB.includePaths += PB.externalIncludePath.value,
    PB.dependentProjectsIncludePaths := protocIncludeDependencies.value,
    PB.targets := PB.targets.?.value.getOrElse(Nil),
    PB.generate := sourceGeneratorTask(PB.generate).dependsOn(PB.unpackDependencies).value,
    PB.runProtoc := { args =>
      com.github.os72.protocjar.Protoc.runProtoc(PB.protocVersion.value +: args.toArray)
    },
    sourceGenerators += PB.generate.taskValue
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    protobufGlobalSettings ++ inConfig(Compile)(protobufConfigSettings)

  case class UnpackedDependencies(dir: File, mappedFiles: Map[File, Seq[File]]) {
    def files: Seq[File] = mappedFiles.values.flatten.toSeq
  }

  private[this] def executeProtoc(
      protocCommand: Seq[String] => Int,
      schemas: Set[File],
      includePaths: Seq[File],
      protocOptions: Seq[String],
      targets: Seq[Target],
      log: Logger
  ): Int =
    try {
      val incPath = includePaths.map("-I" + _.getAbsolutePath)
      protocbridge.ProtocBridge.run(
        protocCommand,
        targets,
        incPath ++ protocOptions ++ schemas.map(_.getAbsolutePath),
        pluginFrontend = protocbridge.frontend.PluginFrontend.newInstance
      )
    } catch {
      case e: Exception =>
        throw new RuntimeException(
          "error occurred while compiling protobuf files: %s" format (e.getMessage),
          e
        )
    }

  private[this] def compile(
      protocCommand: Seq[String] => Int,
      schemas: Set[File],
      includePaths: Seq[File],
      protocOptions: Seq[String],
      targets: Seq[Target],
      deleteTargetDirectory: Boolean,
      log: Logger
  ) = {
    // Sort by the length of path names to ensure that delete parent directories before deleting child directories.
    val generatedTargetDirs = targets.map(_.outputPath).sortBy(_.getAbsolutePath.length)
    generatedTargetDirs.foreach { targetDir =>
      if (deleteTargetDirectory) {
        IO.delete(targetDir)
      }
      targetDir.mkdirs()
    }

    if (schemas.nonEmpty && targets.nonEmpty) {
      log.info(
        "Compiling %d protobuf files to %s".format(schemas.size, generatedTargetDirs.mkString(","))
      )
      log.debug("protoc options:")
      protocOptions.map("\t" + _).foreach(log.debug(_))
      schemas.foreach(schema => log.info("Compiling schema %s" format schema))

      val exitCode =
        executeProtoc(protocCommand, schemas, includePaths, protocOptions, targets, log)
      if (exitCode != 0)
        sys.error("protoc returned exit code: %d" format exitCode)

      log.info("Compiling protobuf")
      generatedTargetDirs.foreach { dir =>
        log.info("Protoc target directory: %s".format(dir.absolutePath))
      }

      targets.flatMap { ot =>
        (ot.outputPath ** ("*.java" | "*.scala")).get
      }.toSet
    } else if (schemas.nonEmpty && targets.isEmpty) {
      log.info("Protobufs files found, but PB.targets is empty.")
      Set[File]()
    } else {
      Set[File]()
    }
  }

  private[this] def unpack(
      deps: Seq[File],
      extractTarget: File,
      streams: TaskStreams
  ): Seq[(File, Seq[File])] = {
    def cachedExtractDep(dep: File): Seq[File] = {
      val cached = FileFunction.cached(
        streams.cacheDirectory / dep.name,
        inStyle = FilesInfo.lastModified,
        outStyle = FilesInfo.exists
      ) { deps =>
        IO.createDirectory(extractTarget)
        deps.flatMap { dep =>
          val set = IO.unzip(dep, extractTarget, "*.proto")
          if (set.nonEmpty) streams.log.debug("Extracted " + set.mkString("\n * ", "\n * ", ""))
          set
        }
      }
      cached(Set(dep)).toSeq
    }

    deps.map { dep =>
      dep -> cachedExtractDep(dep)
    }
  }

  private[this] def isNativePlugin(dep: Attributed[File]): Boolean =
    dep.get(artifact.key).exists(_.`type` == PB.ProtocPlugin)

  private[this] def sourceGeneratorTask(key: TaskKey[Seq[File]]): Def.Initialize[Task[Seq[File]]] =
    Def.task {
      val toInclude = (includeFilter in key).value
      val toExclude = (excludeFilter in key).value
      val schemas = (PB.protoSources in key).value
        .toSet[File]
        .flatMap(
          srcDir =>
            (srcDir ** (toInclude -- toExclude)).get
              .map(_.getAbsoluteFile)
        )
      // Include Scala binary version like "_2.11" for cross building.
      val cacheFile = (streams in key).value.cacheDirectory / s"protobuf_${scalaBinaryVersion.value}"

      val nativePlugins =
        (managedClasspath in (ProtobufConfig, key)).value.filter(isNativePlugin _)

      // Ensure all plugins are executable
      nativePlugins.foreach { dep =>
        dep.data.setExecutable(true)
      }

      val nativePluginsArgs = nativePlugins.map { a =>
        val dep = a.get(artifact.key).get
        s"--plugin=${dep.name}=${a.data.absolutePath}"
      }

      def compileProto(): Set[File] =
        compile(
          (PB.runProtoc in key).value,
          schemas,
          (PB.includePaths in key).value ++ PB.dependentProjectsIncludePaths.value,
          (PB.protocOptions in key).value ++ nativePluginsArgs,
          (PB.targets in key).value,
          (PB.deleteTargetDirectory in key).value,
          (streams in key).value.log
        )

      val cachedCompile = FileFunction.cached(
        cacheFile,
        inStyle = FilesInfo.lastModified,
        outStyle = FilesInfo.exists
      ) { (in: Set[File]) =>
        compileProto()
      }

      if (PB.recompile.value) {
        compileProto().toSeq
      } else {
        cachedCompile(schemas).toSeq
      }
    }

  private[this] def unpackDependenciesTask(key: TaskKey[UnpackedDependencies]) = Def.task {
    val extractedFiles = unpack(
      (managedClasspath in (ProtobufConfig, key)).value.map(_.data),
      (PB.externalIncludePath in key).value,
      (streams in key).value
    )
    UnpackedDependencies((PB.externalIncludePath in key).value, extractedFiles.toMap)
  }

  /**
    * Gets a Seq[File] representing the proto sources of all the projects that the current project depends on.
    */
  def protocIncludeDependencies: Def.Initialize[Seq[File]] = Def.settingDyn {
    val deps = buildDependencies.value.classpath

    def getAllProjectDeps(ref: ProjectRef)(visited: Set[ProjectRef] = Set(ref)): Set[ProjectRef] =
      deps
        .getOrElse(ref, Seq.empty)
        .map(_.project)
        .toSet
        .diff(visited)
        .flatMap(getAllProjectDeps(_)(visited + ref)) + ref

    val thisProjectDeps = getAllProjectDeps(thisProjectRef.value)()

    thisProjectDeps
      .map(ref => (PB.protoSources in (ref, Compile), PB.includePaths in (ref, Compile)))
      .foldLeft(Def.setting(Seq.empty[File])) {
        case (acc, (srcs, includes)) =>
          Def.settingDyn {
            val values = acc.value ++ srcs.?.value.getOrElse(Nil) ++ includes.?.value.getOrElse(Nil)
            Def.setting(values.distinct)
          }
      }
  }

}
