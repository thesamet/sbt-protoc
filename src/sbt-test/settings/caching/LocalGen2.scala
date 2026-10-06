package codegen

import protocgen._
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.File

object LocalGen extends CodeGenApp {
  def process(request: CodeGenRequest): CodeGenResponse = {
    CodeGenResponse.succeed(
      Seq(
        File.newBuilder
          .setName("LocalGen.scala")
          .setContent("object LocalGen { val version = 2 } ")
          .build
      )
    )
  }
}
