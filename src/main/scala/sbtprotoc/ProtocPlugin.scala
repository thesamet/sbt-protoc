package sbtprotoc

import sbt._
import Keys._
import java.io.File

import protocbridge.Target
import sbt.plugins.JvmPlugin


object ProtocPlugin extends AutoPlugin {
  object autoImport {
    object PB {
      val includePaths = SettingKey[Seq[File]]("protoc-include-paths", "The paths that contain *.proto dependencies.")
      val externalIncludePath = SettingKey[File]("protoc-external-include-path", "The path to which protobuf:libraryDependencies are extracted and which is used as protobuf:includePath for protoc")
      val generate = TaskKey[Seq[File]]("protoc-generate", "Compile the protobuf sources.")
      val unpackDependencies = TaskKey[UnpackedDependencies]("protoc-unpack-dependencies", "Unpack dependencies.")
      val protocOptions = SettingKey[Seq[String]]("protoc-options", "Additional options to be passed to protoc")
      val protoSources = SettingKey[Seq[File]]("protoc-sources", "Directory to look for source files")
      val targets = SettingKey[Seq[Target]]("protoc-targets", "List of targets to generate")

      val runProtoc = SettingKey[Seq[String] => Int]("protoc-run-protoc", "A function that executes the protobuf compiler with the given arguments, returning the exit code of the compilation run.")
      val protocVersion = SettingKey[String]("protoc-version", "Version flag to pass to protoc-jar")
      val pythonExe =  SettingKey[String]("python-executable", "Full path for a Python.exe (needed only on Windows)")

      val Target = protocbridge.Target
      val gens = protocbridge.gens
    }
  }

  import autoImport.PB

  val ProtobufConfig = config("protobuf")

  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = JvmPlugin

  override def projectConfigurations: Seq[Configuration] = Seq(ProtobufConfig)

  def protobufGlobalSettings: Seq[Def.Setting[_]] = Seq(
    includeFilter in PB.generate := "*.proto",
    PB.externalIncludePath <<= target(_ / "protobuf_external"),

    libraryDependencies <++= (PB.targets in Compile) (_.flatMap(_.generator.suggestedDependencies.map(makeArtifact))),
    managedClasspath in ProtobufConfig := {
      val artifactTypes: Set[String] = (classpathTypes in ProtobufConfig).value
      Classpaths.managedJars(ProtobufConfig, artifactTypes, (update in ProtobufConfig).value)
    },
    ivyConfigurations += ProtobufConfig,
    watchSources ++= ((PB.protoSources in Compile).value ** "*.proto").get,
    cleanFiles <++= (PB.targets in Compile){_.map{_.outputPath}},
    cleanFiles <+= (PB.externalIncludePath in Compile),
    PB.protocVersion := "-v300",
    PB.runProtoc := { args =>
      com.github.os72.protocjar.Protoc.runProtoc(PB.protocVersion.value +: args.toArray)
    },
    PB.pythonExe := "python"
  )

  // Settings that are applied at configuration (Compile, Test) scope.
  def protobufConfigSettings: Seq[Setting[_]] = Seq(
    PB.protocOptions := Nil,

    PB.unpackDependencies <<= unpackDependenciesTask(PB.unpackDependencies),

    PB.protoSources := Nil,
    PB.protoSources <+= sourceDirectory { _ / "protobuf" },

    PB.includePaths <<= PB.protoSources,
    PB.includePaths <+= PB.externalIncludePath,

    PB.targets := Nil,
    PB.targets += Target(PB.gens.java, sourceManaged.value),

    PB.generate <<= sourceGeneratorTask(PB.generate).dependsOn(PB.unpackDependencies),
    sourceGenerators <+= PB.generate
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    protobufGlobalSettings ++ inConfig(Compile)(protobufConfigSettings)

  case class UnpackedDependencies(dir: File, files: Seq[File])

  private[this] def executeProtoc(protocCommand: Seq[String] => Int, schemas: Set[File], includePaths: Seq[File], protocOptions: Seq[String], targets: Seq[Target], pythonExe: String, log: Logger) : Int =
    try {
      val incPath = includePaths.map("-I" + _.getCanonicalPath)
      protocbridge.ProtocBridge.run(protocCommand, targets,
        incPath ++ protocOptions ++ schemas.map(_.getCanonicalPath),
        pluginFrontend = protocbridge.frontend.PluginFrontend.newInstance(pythonExe=pythonExe))
    } catch { case e: Exception =>
      throw new RuntimeException("error occured while compiling protobuf files: %s" format(e.getMessage), e)
    }

  private def makeArtifact(f: protocbridge.Artifact): ModuleID =
    ModuleID(f.groupId, f.artifactId, f.version, crossVersion =
      if (f.crossVersion) CrossVersion.binary else CrossVersion.Disabled)

  private[this] def compile(protocCommand: Seq[String] => Int, schemas: Set[File], includePaths: Seq[File], protocOptions: Seq[String], targets: Seq[Target], pythonExe: String, log: Logger) = {
    val generatedTargetDirs = targets.map(_.outputPath)
    generatedTargetDirs.foreach{ targetDir =>
      IO.delete(targetDir)
      targetDir.mkdirs()
    }

    if(!schemas.isEmpty){
      log.info("Compiling %d protobuf files to %s".format(schemas.size, generatedTargetDirs.mkString(",")))
      log.debug("protoc options:")
      protocOptions.map("\t"+_).foreach(log.debug(_))
      schemas.foreach(schema => log.info("Compiling schema %s" format schema))

      val exitCode = executeProtoc(protocCommand, schemas, includePaths, protocOptions, targets, pythonExe, log)
      if (exitCode != 0)
        sys.error("protoc returned exit code: %d" format exitCode)

      log.info("Compiling protobuf")
      generatedTargetDirs.foreach{ dir =>
        log.info("Protoc target directory: %s".format(dir.absolutePath))
      }

      (targets.flatMap{ot => (ot.outputPath ** ("*.java" | "*.scala")).get}).toSet
    } else {
      Set[File]()
    }
  }

  private[this] def unpack(deps: Seq[File], extractTarget: File, log: Logger): Seq[File] = {
    IO.createDirectory(extractTarget)
    deps.flatMap { dep =>
      val seq = IO.unzip(dep, extractTarget, "*.proto").toSeq
      if (!seq.isEmpty) log.debug("Extracted " + seq.mkString("\n * ", "\n * ", ""))
      seq
    }
  }

  private[this] def sourceGeneratorTask(key: TaskKey[Seq[File]]): Def.Initialize[Task[Seq[File]]] = Def.task {
    val schemas = (PB.protoSources in key).value.toSet[File].flatMap(srcDir => (srcDir ** ((includeFilter in key).value -- (excludeFilter in key).value)).get
      .map(_.getAbsoluteFile))
    val cachedCompile = FileFunction.cached(
      (streams in key).value.cacheDirectory / "protobuf", inStyle = FilesInfo.lastModified, outStyle = FilesInfo.exists) { (in: Set[File]) =>
      compile(
        (PB.runProtoc in key).value,
        schemas,
        (PB.includePaths in key).value,
        (PB.protocOptions in key).value,
        (PB.targets in key).value,
        (PB.pythonExe in key).value,
        (streams in key).value.log)
    }
    cachedCompile(schemas).toSeq
  }

  private[this] def unpackDependenciesTask(key: TaskKey[UnpackedDependencies]) = Def.task {
    val extractedFiles = unpack((managedClasspath in (ProtobufConfig, key)).value.map(_.data), (PB.externalIncludePath in key).value, (streams in key).value.log)
    UnpackedDependencies((PB.externalIncludePath in key).value, extractedFiles)
  }
}
