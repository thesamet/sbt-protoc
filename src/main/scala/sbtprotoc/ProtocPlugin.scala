package sbtprotoc

import sbt._
import Keys._
import java.io.{File, FileInputStream, IOException}

import protocbridge.{DescriptorSetGenerator, SandboxedJvmGenerator, Target, ProtocRunner}
import sbt.librarymanagement.{CrossVersion, ModuleID}
import sbt.plugins.JvmPlugin
import sbt.util.CacheImplicits

import sjsonnew.JsonFormat
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport.platformDepsCrossVersion
import java.net.URLClassLoader
import java.util.jar.JarInputStream
import sbt.librarymanagement.DependencyResolution
import protocbridge.{Artifact => BridgeArtifact}
import protocbridge.{SystemDetector => BridgeSystemDetector, FileCache, PluginGenerator}
import scala.concurrent.{Future, blocking}
import scala.concurrent.ExecutionContext.Implicits.global

object ProtocPlugin extends AutoPlugin {
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

      val externalSourcePath = SettingKey[File](
        "protoc-external-source-path",
        "The path to which protobuf-src:libraryDependencies are extracted and which is used as additional sources for protoc"
      )

      val generate = TaskKey[Seq[File]]("protoc-generate", "Compile the protobuf sources.")

      val unpackDependencies =
        TaskKey[UnpackedDependencies]("protoc-unpack-dependencies", "Unpack dependencies.")

      val protocOptions =
        SettingKey[Seq[String]]("protoc-options", "Additional options to be passed to protoc")

      val protoSources =
        SettingKey[Seq[File]]("protoc-sources", "Directories to look for source files")

      val manifestProcessing =
        SettingKey[Boolean](
          "protoc-manifest-processing",
          "Enable the automatic addition of scalapb compiler option files from the protoc compile scope"
        )

      val targets = SettingKey[Seq[Target]]("protoc-targets", "List of targets to generate")

      val protocExecutable = TaskKey[File](
        "protoc-executable",
        "Path to a protoc executable. Default downloads protocDependency from maven."
      )

      val runProtoc = TaskKey[ProtocRunner[Int]](
        "protoc-run-protoc",
        "protocbridge.ProtocRunner is a function object that executes protoc with given command line arguments and environment variables, returning the exit code of the compilation run."
      )
      val protocVersion = SettingKey[String]("protoc-version", "Version flag to pass to protoc-jar")

      val protocDependency = SettingKey[ModuleID](
        "protoc-dependency",
        "Binary artifact for protoc on maven"
      )

      val artifactResolver = TaskKey[BridgeArtifact => Seq[java.io.File]](
        "artifact-resolver",
        "Function that retrieves all transitive dependencies of a given artifact."
      )

      val protocCache = TaskKey[FileCache[ModuleID]]("protoc-cache", "Cache of protoc executables")

      val cacheArtifactResolution = SettingKey[Boolean](
        "protoc-cache-artifact-resolution",
        "If false, all sandboxed generators artifacts will be resolved on each invocation (instead of once " +
          "for the entire sbt session).  This is useful when a custom PB.artifactResolver returns different files on " +
          "each invocation."
      )

      @deprecated(
        "Classloaders are now properly invalidated on classpath changes, consider PB.cacheArtifactResolution if you still need to override the default behavior",
        "1.0.1"
      )
      val cacheClassLoaders = SettingKey[Boolean](
        "cache-classloaders",
        "If false, has the same effect as PB.cacheArtifactResolution := false"
      )

      val deleteTargetDirectory = SettingKey[Boolean](
        "delete-target-directory",
        "Delete target directory before regenerating sources."
      )
      val recompile = TaskKey[Boolean]("protoc-recompile")

      val Target       = protocbridge.Target
      val gens         = protocbridge.gens
      val ProtocPlugin = "protoc-plugin"
      val ProtocBinary = "protoc-binary"
    }

    implicit class AsProtocPlugin(val moduleId: ModuleID) extends AnyVal {
      def asProtocPlugin(): ModuleID = {
        moduleId % "protobuf" artifacts (Artifact(
          name = moduleId.name,
          `type` = PB.ProtocPlugin,
          extension = "exe",
          classifier = BridgeSystemDetector.detectedClassifier()
        ))
      }
      def asProtocBinary(): ModuleID = {
        moduleId artifacts (Artifact(
          name = moduleId.name,
          `type` = PB.ProtocBinary,
          extension = "exe",
          classifier = BridgeSystemDetector.detectedClassifier()
        ))
      }
    }

    val ProtocRunner = protocbridge.ProtocRunner

    // Key type for PB.protocRun has changed, this provides
    import scala.language.implicitConversions
    @deprecated("PB.protocRun expects now a protocbridge.ProtocRunner.", "1.0.0")
    implicit def protocRunnerConverter(f: Seq[String] => Int): protocbridge.ProtocRunner[Int] =
      protocbridge.ProtocRunner.fromFunction((args, extraEnv) => f(args))
  }

  private[sbtprotoc] final case class Arguments(
      protocVersion: String,
      includePaths: Seq[File],
      protocOptions: Seq[String],
      deleteTargetDirectory: Boolean,
      targets: Seq[(String, Seq[BridgeArtifact], File, Seq[String])]
  )

  private[sbtprotoc] object Arguments extends CacheImplicits {
    implicit val artifactFormat: JsonFormat[BridgeArtifact] =
      caseClassArray(BridgeArtifact.apply _, BridgeArtifact.unapply _)

    implicit val argumentsFormat: JsonFormat[Arguments] =
      caseClassArray(Arguments.apply _, Arguments.unapply _)
  }

  import autoImport.PB
  import autoImport.AsProtocPlugin

  val ProtobufConfig = config("protobuf")

  val ProtobufSrcConfig = config("protobuf-src")

  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = JvmPlugin

  override def projectConfigurations: Seq[Configuration] = Seq(ProtobufConfig)

  override def globalSettings: Seq[Def.Setting[_]] = protobufGlobalSettings

  private[this] def protobufGlobalSettings: Seq[Def.Setting[_]] =
    Seq(
      PB.protocVersion            := "3.19.2",
      PB.deleteTargetDirectory    := true,
      PB.cacheArtifactResolution  := true,
      PB.cacheClassLoaders        := true,
      PB.generate / includeFilter := "*.proto",
      PB.generate / dependencyResolution := {
        val log = streams.value.log

        val rootDependencyResolution =
          (LocalRootProject / dependencyResolution).?.value

        rootDependencyResolution.getOrElse {
          log.warn(
            "Falling back on a default `dependencyResolution` to " +
              "resolve sbt-protoc plugins because `dependencyResolution` " +
              "is not set in the root project of this build."
          )
          log.warn(
            "Consider explicitly setting " +
              "`Global / PB.generate / dependencyResolution` " +
              "instead of relying on the default."
          )

          import sbt.librarymanagement.ivy._
          val ivyConfig = InlineIvyConfiguration()
            .withResolvers(Vector(Resolver.defaultLocal, Resolver.mavenCentral))
            .withLog(log)
          IvyDependencyResolution(ivyConfig)
        }
      },
      PB.protocCache := {
        val log = streams.value.log
        val lm  = (PB.generate / dependencyResolution).value

        def downloadArtifact(targetDirectory: File, moduleId: ModuleID): Future[File] =
          Future {
            blocking {
              lm.retrieve(
                moduleId,
                None,
                targetDirectory,
                log
              ).fold(w => throw w.resolveException, _.head)
            }
          }

        def cacheKey(moduleId: ModuleID): String = {
          val classifier = moduleId.explicitArtifacts.headOption.flatMap(_.classifier).getOrElse("")
          val ext        = if (classifier.startsWith("win")) ".exe" else ""
          s"${moduleId.name}-$classifier-${moduleId.revision}$ext"
        }

        new protocbridge.FileCache[ModuleID](
          protocbridge.FileCache.cacheDir,
          downloadArtifact,
          cacheKey
        )
      },
      PB.artifactResolver := artifactResolverImpl(
        (PB.generate / dependencyResolution).value,
        streams.value.cacheDirectory / "sbt-protoc",
        streams.value.log
      )
    )

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(Compile, Test).flatMap(inConfig(_)(protobufConfigSettings)) ++
      protobufProjectSettings

  private[this] val protobufProjectSettings: Seq[Def.Setting[_]] =
    Seq(
      PB.externalIncludePath := target.value / "protobuf_external",
      PB.externalSourcePath  := target.value / "protobuf_external_src",
      Compile / PB.protoSources += PB.externalSourcePath.value,
      PB.unpackDependencies := unpackDependenciesTask(PB.unpackDependencies).value,
      PB.additionalDependencies := {
        val libs = (Compile / PB.targets).value.flatMap(_.generator.suggestedDependencies)
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
      ProtobufConfig / classpathTypes += PB.ProtocPlugin,
      ProtobufConfig / managedClasspath :=
        Classpaths.managedJars(
          ProtobufConfig,
          (ProtobufConfig / classpathTypes).value,
          (ProtobufConfig / update).value
        ),
      ProtobufSrcConfig / managedClasspath :=
        Classpaths.managedJars(
          ProtobufSrcConfig,
          (ProtobufSrcConfig / classpathTypes).value,
          (ProtobufSrcConfig / update).value
        ),
      ivyConfigurations ++= Seq(ProtobufConfig, ProtobufSrcConfig),
      PB.protocDependency := {
        val version =
          if (PB.protocVersion.value.startsWith("-v")) {
            sLog.value.warn(
              "The PB.protocVersion setting no longer requires '-v` prefix. The prefix has been " +
                "automatically removed. This warning will turn into an error in a future version."
            )
            PB.protocVersion.value.substring(2)
          } else PB.protocVersion.value
        if (version.indexOf('.') == -1) {
          throw new IllegalArgumentException(
            s"""PB.protocVersion must contain a dot-separated version number. For example: "3.13.0". Got: '${PB.protocVersion.value}'"""
          )
        }
        ("com.google.protobuf" % "protoc" % version) asProtocBinary (),
      },
      PB.protocExecutable := {
        scala.concurrent.Await.result(
          PB.protocCache.value.get(PB.protocDependency.value),
          scala.concurrent.duration.Duration.Inf
        )
      }
    )

  // Settings that are applied at configuration (Compile, Test) scope.
  val protobufConfigSettings: Seq[Setting[_]] =
    Seq(
      PB.recompile     := false,
      PB.protocOptions := Nil,
      PB.targets       := Nil,
      PB.protoSources  := Nil,
      PB.protoSources += sourceDirectory.value / "protobuf",
      PB.manifestProcessing := true,
      PB.includePaths := (
        PB.includePaths.?.value.getOrElse(Nil) ++
          PB.protoSources.value ++
          Seq(PB.externalIncludePath.value, PB.externalSourcePath.value) ++
          protocIncludeDependencies.value,
      ).distinct,
      PB.generate := sourceGeneratorTask(PB.generate)
        .dependsOn(
          // We need to unpack dependencies for all subprojects since current project is allowed to import
          // them.
          PB.unpackDependencies.?.all(
            ScopeFilter(
              inDependencies(ThisProject, transitive = false),
              inConfigurations(Compile)
            )
          )
        )
        .value,
      PB.runProtoc := Def.taskDyn {
        val s = streams.value
        if (PB.targets.value == Nil) {
          Def.task {
            // return a dummy instance that should never be evaluated
            protocbridge.ProtocRunner.fromFunction[Int] { (_, _) =>
              throw new RuntimeException("protoc was not resolved")
            }
          }
        } else
          Def.task {
            val exec = PB.protocExecutable.value.getAbsolutePath.toString
            (protocbridge.ProtocRunner.fromFunction { (args, extraEnv) =>
              s.log.debug(
                s"Executing protoc with ${args.mkString("[", ", ", "]")} and extraEnv=$extraEnv"
              )
              ()
            } zip protocbridge.ProtocRunner(exec)).map(_._2)
          }
      }.value,
      sourceGenerators += PB.generate
        .map(_.filter { file =>
          val name = file.getName
          name.endsWith(".java") || name.endsWith(".scala")
        })
        .taskValue,
      unmanagedResourceDirectories ++= PB.protoSources.value
        .filterNot(_ == PB.externalSourcePath.value),
      unmanagedSourceDirectories ++= PB.protoSources.value
        .filterNot(_ == PB.externalSourcePath.value)
    )

  case class UnpackedDependency(files: Seq[File], optionProtos: Seq[File])

  case class UnpackedDependencies(mappedFiles: Map[File, UnpackedDependency]) {
    def files: Seq[File] = mappedFiles.values.flatMap(_.files).toSeq
  }

  private[sbtprotoc] object UnpackedDependencies extends CacheImplicits {
    implicit val UnpackedDependencyFormat: JsonFormat[UnpackedDependency] =
      caseClassArray(UnpackedDependency.apply _, UnpackedDependency.unapply _)

    implicit val UnpackedDependenciesFormat: JsonFormat[UnpackedDependencies] =
      caseClassArray(UnpackedDependencies.apply _, UnpackedDependencies.unapply _)
  }

  private[this] def artifactResolverImpl(
      lm: DependencyResolution,
      cacheDirectory: File,
      log: Logger
  )(artifact: BridgeArtifact) = {
    lm
      .retrieve(
        lm.wrapDependencyInModule(makeArtifact(artifact)),
        cacheDirectory,
        log
      )
      .fold(w => throw w.resolveException, identity(_))
  }

  private[this] def executeProtoc(
      protocRunner: ProtocRunner[Int],
      schemas: Set[File],
      includePaths: Seq[File],
      protocOptions: Seq[String],
      targets: Seq[Target],
      sandboxedLoader: BridgeArtifact => ClassLoader
  ): Int =
    try {
      // Remove inexistent paths from protoc command line to avoid warnings.
      val incPath = includePaths.collect {
        case f if f.exists() => "-I" + f.getAbsolutePath
      }
      protocbridge.ProtocBridge.execute(
        protocRunner,
        targets,
        incPath ++ protocOptions ++ schemas.toSeq
          .map(_.getAbsolutePath)
          .sorted, // sorted to ensure consistent ordering between calls
        classLoader = sandboxedLoader
      )
    } catch {
      case e: Exception =>
        throw new RuntimeException(
          "error occurred while compiling protobuf files: %s" format (e.getMessage),
          e
        )
    }

  private[this] def sandboxedClassLoader(files: Seq[File]): URLClassLoader = {
    val cloader = new URLClassLoader(
      files.map(_.toURI().toURL()).toArray,
      new FilteringClassLoader(getClass().getClassLoader())
    )
    cloader
  }

  private[this] def compile(
      protocRunner: ProtocRunner[Int],
      schemas: Set[File],
      includePaths: Seq[File],
      protocOptions: Seq[String],
      targets: Seq[Target],
      deleteTargetDirectory: Boolean,
      log: Logger,
      sandboxedLoader: BridgeArtifact => ClassLoader
  ) = {
    val targetPaths = targets.map(_.outputPath).toSet

    if (deleteTargetDirectory) {
      targetPaths.foreach(IO.delete)
    }

    targets
      .map {
        case Target(DescriptorSetGenerator(), outputFile, _) => outputFile.getParentFile
        case Target(_, outputDirectory, _)                   => outputDirectory
      }
      .toSet[File]
      .foreach(_.mkdirs)

    if (schemas.nonEmpty && targets.nonEmpty) {
      log.info(
        "Compiling %d protobuf files to %s".format(schemas.size, targetPaths.mkString(","))
      )
      log.debug("protoc options:")
      protocOptions.map("\t" + _).foreach(log.debug(_))
      schemas.foreach(schema => log.debug("Compiling schema %s" format schema))

      val exitCode =
        executeProtoc(protocRunner, schemas, includePaths, protocOptions, targets, sandboxedLoader)
      if (exitCode != 0)
        sys.error("protoc returned exit code: %d" format exitCode)

      targets.flatMap {
        case Target(DescriptorSetGenerator(), outputFile, _) => Seq(outputFile)
        case Target(_, outputDirectory, _)                   => outputDirectory.allPaths.get
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
  ): Seq[(File, UnpackedDependency)] = {
    def cachedExtractDep(dep: File): Seq[File] = {
      val cached = FileFunction.cached(
        streams.cacheDirectory / dep.name,
        inStyle = FilesInfo.lastModified,
        outStyle = FilesInfo.exists
      ) { deps =>
        IO.createDirectory(extractTarget)
        deps.flatMap { dep =>
          val set = IO.unzip(dep, extractTarget, "*.proto")
          if (set.nonEmpty)
            streams.log.debug("Extracted from " + dep + set.mkString(":\n * ", "\n * ", ""))
          set
        }
      }
      cached(Set(dep)).toSeq
    }

    deps.map { dep =>
      val fileSet = cachedExtractDep(dep)
      dep ->
        UnpackedDependency(fileSet, getOptionProtos(dep, extractTarget, fileSet))
    }
  }

  // Unpacked proto jars may contain a key in their manifest to tell us about
  // the generator options used to generate the sources. The option will be
  // sourced into ScalaPB.
  def getOptionProtos(jar: File, extractTarget: File, fileSet: Seq[File]): Seq[File] = {
    val jin = new JarInputStream(new FileInputStream(jar))
    try {
      val optionProtos = (for {
        manifest     <- Option(jin.getManifest())
        attrs        <- Option(manifest.getMainAttributes())
        optionProtos <- Option(attrs.getValue("Scalapb-Options-Proto"))
      } yield optionProtos.split(',').toSeq)
        .getOrElse(Seq.empty)
        .map(new File(extractTarget, _))

      optionProtos.foreach { optionProto =>
        if (!fileSet.contains(optionProto)) {
          throw new IOException(
            s"Dependency $jar manifest references a non-existant proto $optionProto"
          )
        }
      }

      optionProtos

    } finally {
      jin.close()
    }
  }

  private[this] def isNativePlugin(dep: Attributed[File]): Boolean =
    dep.get(artifact.key).exists(_.`type` == PB.ProtocPlugin)

  private[this] val classloaderCache =
    new java.util.concurrent.ConcurrentHashMap[
      BridgeArtifact,
      (FilesInfo[ModifiedFileInfo], URLClassLoader)
    ]

  private[this] def sourceGeneratorTask(key: TaskKey[Seq[File]]): Def.Initialize[Task[Seq[File]]] =
    Def.task {
      val log       = (key / streams).value.log
      val resolver  = (key / PB.artifactResolver).value
      val cache     = (key / PB.cacheClassLoaders).value && (key / PB.cacheArtifactResolution).value
      val targets   = (key / PB.targets).value
      val toInclude = (key / includeFilter).value
      val toExclude = (key / excludeFilter).value
      val schemas = (key / PB.protoSources).value
        .toSet[File]
        .flatMap(srcDir =>
          (srcDir ** (toInclude -- toExclude)).get
            .map(_.getAbsoluteFile)
        ) match {
        case protos if protos.nonEmpty =>
          val processManifests = (key / PB.manifestProcessing).value
          val dependencies     = (key / PB.unpackDependencies).value

          if (!processManifests) protos
          else {
            val optionProtos = dependencies.mappedFiles.values.flatMap(_.optionProtos)
            protos ++ optionProtos
          }
        case _ => Set.empty[File]
      }

      // Include Scala binary version like "_2.11" for cross building.
      val cacheFile =
        (key / streams).value.cacheDirectory / s"protobuf_${scalaBinaryVersion.value}"

      val nativePlugins =
        (ProtobufConfig / key / managedClasspath).value.filter(isNativePlugin _)

      // Ensure all plugins are executable
      nativePlugins.foreach { dep => dep.data.setExecutable(true) }

      val nativePluginsArgs = nativePlugins.flatMap { a =>
        val dep = a.get(artifact.key).get
        val pluginPath = {
          ProtocRunner
            .maybeNixDynamicLinker()
            .filterNot(_ => a.data.getName.endsWith(".sh")) match {
            case None => a.data.absolutePath
            case Some(linker) =>
              IO.withTemporaryFile("nix", dep.name, keepFile = true) { f =>
                f.deleteOnExit()
                IO.write(f, s"""#!/bin/sh\n$linker ${a.data.absolutePath} "$$@"\n""")
                f.setExecutable(true)
                f.getAbsolutePath()
              }
          }
        }
        val suffixed = PB.targets.value.zipWithIndex.collect {
          case (Target(PluginGenerator(name, _, None), _, _), i)
              if s"protoc-gen-$name" == dep.name =>
            s"--plugin=${dep.name}_$i=${pluginPath}"
        }
        if (suffixed.length == 1) Seq(s"--plugin=${dep.name}=${pluginPath}")
        else suffixed
      }

      // Prepare memoized classloaders for all artifacts referenced by targets. For each artifact, classpath is deeply
      // stamped and compared with the previous stamp to reload the classloader upon changes. Artifact resolution is
      // memoized across invocations for the entire sbt session, unless cacheArtifactResolution is false.
      val stampedClassLoadersByArtifact
          : Map[BridgeArtifact, (FilesInfo[ModifiedFileInfo], ClassLoader)] =
        targets
          .collect { case Target(SandboxedJvmGenerator(_, artifact, _, _), _, _) => artifact }
          .distinct
          .map { artifact =>
            artifact -> classloaderCache.compute(
              artifact,
              { (_, prevValue) =>
                def stampClasspath(files: Seq[File]) =
                  // artifact paths can be JARs or directories, so a recursive stamp is needed
                  FileInfo.lastModified(files.toSet[File].allPaths.get.toSet)

                if (prevValue == null) {
                  // first time this classpath is requested since the start of sbt
                  val resolved = resolver(artifact)
                  (stampClasspath(resolved), sandboxedClassLoader(resolved))
                } else {
                  val (prevFilesInfo, prevClassLoader) = prevValue
                  val currentFiles =
                    if (!cache) resolver(artifact)
                    else {
                      // use classpath as referenced by the previous classloader (which may contain directories)
                      // rather than the previous files captured in the stamp so that any potential new file is
                      // taken into account in the current stamp.
                      prevClassLoader.getURLs.toSeq.map(url => file(url.toURI.getPath))
                    }
                  val currentFilesInfo = stampClasspath(currentFiles)
                  if (currentFilesInfo == prevFilesInfo) prevValue
                  else {
                    log.debug(s"Reloading classloader for $artifact (classpath was updated)")
                    (currentFilesInfo, sandboxedClassLoader(currentFiles))
                  }
                }
              }
            )
          }
          .toMap

      val arguments = Arguments(
        PB.protocVersion.value,
        (key / PB.includePaths).value,
        protocOptions = (key / PB.protocOptions).value ++ nativePluginsArgs,
        deleteTargetDirectory = (key / PB.deleteTargetDirectory).value,
        targets = targets.map { target =>
          (
            target.generator.name,
            target.generator.suggestedDependencies,
            target.outputPath,
            target.options
          )
        }
      )

      def compileProto(): Set[File] = {
        val sandboxedLoader: BridgeArtifact => ClassLoader =
          stampedClassLoadersByArtifact.mapValues(_._2)
        compile(
          (key / PB.runProtoc).value,
          schemas,
          arguments.includePaths,
          arguments.protocOptions,
          targets,
          arguments.deleteTargetDirectory,
          log,
          sandboxedLoader
        )
      }

      import CacheImplicits._
      type Stamp = (Arguments, Seq[FilesInfo[ModifiedFileInfo]])
      val cachedCompile = Tracked.inputChanged[Stamp, Set[File]](
        cacheFile / "input"
      ) { case (inChanged, _) =>
        Tracked.diffOutputs(
          cacheFile / "output",
          FileInfo.exists
        ) { outDiff: ChangeReport[File] =>
          if (inChanged || outDiff.modified.nonEmpty) {
            log.debug {
              val inputInvalidation =
                if (inChanged) Seq("input changed")
                else Seq()
              val outputInvalidation =
                if (outDiff.modified.nonEmpty) Seq(s"output files changed ${outDiff.modified}")
                else Seq()
              s"Invalidating cache (${(inputInvalidation ++ outputInvalidation).mkString(" and ")})"
            }
            compileProto()
          } else outDiff.checked
        }
      }

      if (PB.recompile.value) {
        log.debug("Ignoring cache (PB.recompile := true)")
        compileProto().toSeq
      } else {
        val sandboxedArtifactsStamps =
          stampedClassLoadersByArtifact.values.map(_._1).toSeq
        val inputStamp =
          FileInfo.lastModified(schemas ++ arguments.includePaths.allPaths.get)
        cachedCompile((arguments, sandboxedArtifactsStamps :+ inputStamp)).toSeq
      }
    }

  private[this] def unpackDependenciesTask(key: TaskKey[UnpackedDependencies]) =
    Def.task {
      val extractedFiles = unpack(
        (ProtobufConfig / key / managedClasspath).value.map(_.data),
        (key / PB.externalIncludePath).value,
        (key / streams).value
      )
      val extractedSrcFiles = unpack(
        (ProtobufSrcConfig / key / managedClasspath).value.map(_.data),
        (key / PB.externalSourcePath).value,
        (key / streams).value
      )
      val unpackedDeps = UnpackedDependencies(
        (extractedFiles ++ extractedSrcFiles).toMap
      )

      // clean up stale files that are no longer pulled by a dependency
      val previouslyExtractedFiles = key.previous.map(_.files).toSeq.flatten
      IO.delete(previouslyExtractedFiles.diff(unpackedDeps.files))

      unpackedDeps
    }

  def protocIncludeDependencies: Def.Initialize[Seq[File]] =
    Def.setting {
      (
        PB.protoSources.?.all(filter).value.map(_.getOrElse(Nil)).flatten ++
          PB.includePaths.?.all(filter).value.map(_.getOrElse(Nil)).flatten
      ).distinct
    }

  private[this] def filter: ScopeFilter =
    ScopeFilter(inDependencies(ThisProject, includeRoot = false), inConfigurations(Compile))

  private[this] def makeArtifact(f: BridgeArtifact): ModuleID = {
    ModuleID(f.groupId, f.artifactId, f.version)
      .cross(if (f.crossVersion) CrossVersion.binary else CrossVersion.disabled)
      .withExtraAttributes(f.extraAttributes)
  }

}
