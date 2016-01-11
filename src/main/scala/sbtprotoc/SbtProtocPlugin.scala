package sbtprotoc

import protocbridge._
import sbt.Keys._
import sbt.Scoped.RichTaskable7
import sbt._
import sbt.plugins.JvmPlugin

object SbtProtocPlugin extends AutoPlugin {

  object autoImport {

    object PB {
      val includePaths = TaskKey[Seq[File]]("protoc-include-paths", "The paths that contain *.proto dependencies.")
      val protoc = SettingKey[String]("protoc-protoc", "The path+name of the protoc executable.")
      val runProtoc = TaskKey[Seq[String] => Int]("protoc-run-protoc", "A function that executes the protobuf compiler with the given arguments, returning the exit code of the compilation run.")
      val externalIncludePath = SettingKey[File]("protoc-external-include-path", "The path to which protobuf:library-dependencies are extracted and which is used as protobuf:include-path for protoc")
      val generate = TaskKey[Seq[File]]("protoc-generate", "Compile the protobuf sources.")
      val unpackDependencies = TaskKey[UnpackedDependencies]("protoc-unpack-dependencies", "Unpack dependencies.")
      val protocOptions = SettingKey[Seq[String]]("protoc-protoc-options", "Additional options to be passed to protoc")

      val generatorParams = SettingKey[Seq[GeneratorParam]]("protoc-generator-params", "Targets for protoc: target directory and glob for generated source files")

      val generators = SettingKey[Seq[Generator]]("protoc-generators", "Targets for protoc: target directory and glob for generated source files")

      // Frontend configuration
      val protocFrontend = TaskKey[PluginFrontend]("protoc-plugin-frontend", "Protoc frontend")
      val pythonExecutable = SettingKey[String]("protoc-python-executable", "Full path for a Python.exe (needed only on Windows)")

      // Convenience
      val gens = protocbridge.GeneratorParam.builtin
      val protocConfig = config("protoc")
    }

  }

  import autoImport.PB._

  override def trigger = noTrigger

  override def requires = JvmPlugin

  override def projectConfigurations: Seq[Configuration] = Seq(protocConfig)

  override def projectSettings: Seq[Setting[_]] = inConfig(protocConfig)(Seq(
    sourceDirectory <<= (sourceDirectory in Compile) {
      _ / "protobuf"
    },
    sourceDirectories <<= sourceDirectory apply (_ :: Nil),

    externalIncludePath <<= target(_ / "protobuf_external"),
    protoc := "protoc",
    runProtoc <<= (protoc, streams) map ((cmd, s) => args => Process(cmd, args) ! s.log),

    protocOptions := Nil,

    managedClasspath <<= (classpathTypes, update) map { (ct, report) =>
      Classpaths.managedJars(protocConfig, ct, report)
    },

    unpackDependencies <<= unpackDependenciesTask,

    includePaths <<= (sourceDirectory in protocConfig) map (identity(_) :: Nil),
    includePaths <+= externalIncludePath map identity,

    generators := Nil,

    generatorParams <<= (sourceManaged in Compile, generators in protocConfig) {
      (sm, gs) =>
        gs.map(GeneratorParam(_, sm))
    },

    // Frontend
    pythonExecutable := "python",
    protocFrontend <<= protocFrontendTask,

    generate <<= sourceGeneratorTask.dependsOn(unpackDependencies)
  )) ++ Seq[Setting[_]](
    sourceGenerators in Compile <+= generate in protocConfig,
    cleanFiles <++= (generatorParams in protocConfig) {
      _.map {
        _.outputPath
      }
    },
    cleanFiles <+= (externalIncludePath in protocConfig),
    managedSourceDirectories in Compile <++= (generatorParams in protocConfig) {
      _.map {
        _.outputPath
      }
    },
    libraryDependencies <++= (generatorParams in protocConfig) (_.flatMap(_.suggestedDependencies.map(makeArtifact)))
  )

  case class UnpackedDependencies(dir: File, files: Seq[File])

  private def makeArtifact(f: protocbridge.Artifact): ModuleID =
    ModuleID(f.groupId, f.artifactId, f.version, crossVersion =
      if (f.crossVersion) CrossVersion.binary else CrossVersion.Disabled)

  private def executeProtoc(protocCommand: Seq[String] => Int, schemas: Set[File], includePaths: Seq[File], protocOptions: Seq[String],
                            generators: Seq[GeneratorParam], log: Logger): Int =
    try {
      val incPath = includePaths.map("-I" + _.absolutePath)
      val args = incPath ++ protocOptions ++ schemas.map(_.absolutePath)
      protocbridge.ProtocBridge.run(protocCommand, args, generators)
    } catch {
      case e: Exception =>
        throw new RuntimeException("error occured while compiling protobuf files: %s".format(e.getMessage), e)
    }

  private def compile(protocCommand: Seq[String] => Int, schemas: Set[File], includePaths: Seq[File], protocOptions: Seq[String],
                      generators: Seq[GeneratorParam], log: Logger): Set[File] = {
    val generatedTargetDirs = generators.map(_.outputPath)

    generatedTargetDirs.foreach(_.mkdirs())

    log.info("Compiling %d protobuf files to %s".format(schemas.size, generatedTargetDirs.mkString(",")))
    log.debug("protoc options:")
    protocOptions.map("\t" + _).foreach(log.debug(_))
    schemas.foreach(schema => log.info("Compiling schema %s" format schema))

    val exitCode = executeProtoc(
      protocCommand, schemas, includePaths, protocOptions, generators, log)

    if (exitCode != 0)
      sys.error("protoc returned exit code: %d" format exitCode)

    log.info("Compiling protobuf")

    generatedTargetDirs.foreach { dir =>
      log.info("Protoc target directory: %s".format(dir.absolutePath))
    }

    generators.flatMap { ot =>
      (ot.outputPath ** "*.java").get ++
        (ot.outputPath ** "*.scala").get
    }.toSet
  }

  private def unpack(deps: Seq[File], extractTarget: File, log: Logger): Seq[File] = {
    IO.createDirectory(extractTarget)
    deps.flatMap { dep =>
      val seq = IO.unzip(dep, extractTarget, "*.proto").toSeq
      if (seq.nonEmpty) log.debug("Extracted " + seq.mkString("\n * ", "\n * ", ""))
      seq
    }
  }

  private def sourceGeneratorTask: RichTaskable7[TaskStreams, Seq[File], Seq[File], Seq[String], Seq[GeneratorParam], File, (Seq[String]) => Int]#App[Seq[File]] =
    (streams, sourceDirectories in protocConfig, includePaths in protocConfig, protocOptions in protocConfig, generatorParams in protocConfig, cacheDirectory, runProtoc) map {
      (out, srcDirs, includePaths, protocOpts, otherTargets, cache, protocCommand) =>
        val schemas: Set[File] = srcDirs.toSet[File].flatMap(srcDir => (srcDir ** "*.proto").get.map(_.getAbsoluteFile))
        val cachedCompile = FileFunction.cached(cache / "protobuf", inStyle = FilesInfo.lastModified, outStyle = FilesInfo.exists) { (in: Set[File]) =>
          compile(protocCommand, schemas, includePaths, protocOpts, otherTargets, out.log)
        }
        cachedCompile(schemas).toSeq
    }

  private def unpackDependenciesTask = (streams, managedClasspath in protocConfig, externalIncludePath in protocConfig) map {
    (out, deps, extractTarget) =>
      val extractedFiles = unpack(deps.map(_.data), extractTarget, out.log)
      UnpackedDependencies(extractTarget, extractedFiles)
  }

  private def protocFrontendTask: Def.Initialize[Task[PluginFrontend]] = (pythonExecutable in protocConfig) map {
    def isWindows: Boolean = sys.props("os.name").startsWith("Windows")
    pythonExecutable =>
      if (isWindows) new WindowsPluginFrontend(pythonExecutable)
      else PosixPluginFrontend
  }
}
