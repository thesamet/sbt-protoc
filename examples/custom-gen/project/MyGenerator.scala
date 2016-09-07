import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.Descriptors._
import com.google.protobuf.compiler.PluginProtos.{CodeGeneratorResponse, CodeGeneratorRequest}
import scala.collection.JavaConverters._

import com.trueaccord.scalapb.compiler.{DescriptorPimps, FunctionalPrinter}

/** This is the interface that code generators need to implement. */
object MyCodeGenerator extends protocbridge.ProtocCodeGenerator with DescriptorPimps {
  val params = com.trueaccord.scalapb.compiler.GeneratorParams()

  def run(request: CodeGeneratorRequest): CodeGeneratorResponse = {
    val b = CodeGeneratorResponse.newBuilder

    val fileDescByName: Map[String, FileDescriptor] =
      request.getProtoFileList.asScala.foldLeft[Map[String, FileDescriptor]](Map.empty) {
        case (acc, fp) =>
          val deps = fp.getDependencyList.asScala.map(acc)
          acc + (fp.getName -> FileDescriptor.buildFrom(fp, deps.toArray))
      }

    request.getFileToGenerateList.asScala.foreach {
      name =>
        val fileDesc = fileDescByName(name)
        val responseFile = generateFile(fileDesc)
        b.addFile(responseFile)
    }
    b.build
  }

  def generateFile(fileDesc: FileDescriptor): CodeGeneratorResponse.File = {
    val b = CodeGeneratorResponse.File.newBuilder()
    b.setName(s"${fileDesc.scalaDirectory}/${fileDesc.fileDescriptorObjectName}Foo.scala")
    val fp = FunctionalPrinter()
      .add(s"package ${fileDesc.scalaPackageName}")
      .add("")
      .print(fileDesc.getMessageTypes.asScala) {
        case (m, p) =>
          p.add(s"object ${m.getName}Boo {")
            .indent
            .add(s"type T = ${m.scalaTypeName}")
            .add(s"val FieldCount = ${m.getFields.size}")
            .outdent
            .add("}")
      }
      b.setContent(fp.result)
      b.build
  }
}
