sbt-protoc
==========
[![CI](https://github.com/thesamet/sbt-protoc/workflows/CI/badge.svg)](https://github.com/thesamet/sbt-protoc/actions?query=workflow%3ACI)

This plugin uses protoc to generate code from proto files. This SBT plugin is
meant to supersede
[sbt-protobuf](https://github.com/sbt/sbt-protobuf/) and
[sbt-scalapb](https://github.com/scalapb/sbt-scalapb).

Highlights
----------

1. Uses protoc-jar by default (no need to install protoc)
2. Generates source code directly under `src_managed` by default (works
   better with IntelliJ)
3. Supports compiling protos in both `test` and `compile` configuration.
4. Supports JVM-based code generators. [Write your own custom code generator.](https://github.com/thesamet/sbt-protoc/tree/master/examples/custom-gen)
5. Straightforward: No `PB.protobufSettings`, packaged as auto-plugin.

Installation
------------

**Step 1: create `project/protoc.sbt` with:**

```scala
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.25")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.9.0"
```

**Step 2: add to `build.sbt`:**

If you only want to generate Java code:

```scala
PB.targets in Compile := Seq(
  PB.gens.java -> (sourceManaged in Compile).value
)
```

A version of `protobuf-java` is going to get added to the runtime
dependencies. To explicitly set this version you can write:

```scala
PB.targets in Compile := Seq(
  PB.gens.java("3.7.0") -> (sourceManaged in Compile).value
)
```

For ScalaPB:
```scala
PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)
```

To generate Java + Scala with Java conversions:
```scala
PB.targets in Compile := Seq(
  PB.gens.java -> (sourceManaged in Compile).value,
  scalapb.gen(javaConversions = true) -> (sourceManaged in Compile).value
)
```

## Download plugins that are available on maven repository

To download an artifact and use it as a code generator plugin:

```scala
libraryDependencies += "io.grpc" % "protoc-gen-grpc-java" % "1.23.0" asProtocPlugin()

PB.targets in Compile := Seq(
  PB.gens.plugin("grpc-java") -> (sourceManaged in Compile).value,
)
```

Note the `asProtocPlugin` provided to the dependency, this is equivalent to:
```
libraryDependencies += "io.grpc" % "protoc-gen-grpc-java" % "1.23.0" % "protobuf" artifacts(
  Artifact("protoc-gen-grpc-java", PB.ProtocPlugin, "exe", "linux-x86_64"))
```

with the operating system replaced accordingly to the system you are running on. You can use the
full syntax in case the code generator you are trying to download follows a
different pattern.

## To invoke a plugin that is already locally installed

    PB.targets in Compile := Seq(
      PB.gens.plugin(name="myplugin", path="/path/to/plugin") -> (sourceManaged in Compile).value / "js"
    )

If you need to pass parameters to the plugin, it can be done as follows:

    val grpcWebGen = PB.gens.plugin(
      name="grpc-web",
      path="/usr/local/bin/protoc-gen-grpc-web-1.0.7-linux-x86_64"
    )

    PB.targets in Compile := Seq(
      (grpcWebGen, Seq("mode=grpcwebtext")) -> (sourceManaged in Compile).value / "js"
    )

**Step 3: Put some protos in src/main/protobuf and compile**

Additional options
------------------

The options below need to be scoped to either `Compile` or `Test` (if unsure,
you probably want `Compile`)

Example settings:
```scala
// Force the version for the protoc binary
PB.protocVersion := "3.11.4"

// Additional directories to search for imports:
PB.includePaths in Compile ++= Seq(file("/some/other/path"))

// Make protos from some Jar available to import.
libraryDependencies ++= Seq(
  "com.google.protobuf" % "protobuf-java" % "3.7.0" % "protobuf"
)

// Compile protos from some Jar (and make them available to import). Without
// the intrasitive() directory, `protobuf-src` would also unpack and compile
// all transitive dependencies of the package. This could lead to having
// duplicate class files, if another library is already providing compiled
// classes for your dependencies.
libraryDependencies ++= Seq(
  "com.google.api.grpc" % "proto-google-common-protos" % "1.17.0" % "protobuf-src" intransitive()
)

// Changing where to look for protos to compile (default src/main/protobuf):
PB.protoSources in Compile := Seq(sourceDirectory.value / "somewhere")

// Additional options to pass to protoc:
PB.protocOptions in Compile := Seq("-xyz")

// Excluding some proto files:
excludeFilter in PB.generate := "test-*.proto"

// Rarely needed: override where proto files from library dependencies are
// extracted to:
PB.externalIncludePath := file("/tmp/foo")

// By default we generate into target/src_managed. To customize:
PB.targets in Compile := Seq(
  scalapb.gen() -> file("/some/other/dir")
)

// Use a locally provided protoc (in 1.x):
PB.protocExecutable := file("/path/to/protoc")
// In <1.0:
PB.runProtoc in Compile := (args => Process("/path/to/protoc", args)!)

// Prevents the plugin from adding libraryDependencies to your project
PB.additionalDependencies := Nil

// Before version 0.99.15, when compiling in Windows, Python was used to bridge
// protoc and this JVM. To set the path for Python.exe:
// Note that this must be Python2 and not Python3.
// Since version 0.99.15 this option has no effect, and will be removed in a
// future version.
PB.pythonExe := "/path/to/python.exe"
```

Protos in other configs
-----------------------

This plugin supports generating protos in the `Test` config. That means, that
you can put protos under `src/test/protobuf` and have it generated and compiled under the
`Test` configuration, so the generated code is only available to your tests,
but not to your main code.

To do that, add:

    PB.targets in Test := Seq(
        PB.gens.java("3.11.4") -> (sourceManaged in Test).value
    )

If you want to have protos in some other configuration (not `Compile` or
`Test`), for example `IntegrationTest` you need to manually add the plugin
default settings in that configuration:

```scala
configs(IntegrationTest)

inConfig(IntegrationTest)(sbtprotoc.ProtocPlugin.protobufConfigSettings)

PB.targets in IntegrationTest := Seq(
    PB.gens.java("3.11.4") -> (sourceManaged in IntegrationTest).value
)
```

Debugging
---------

Show proto files extracted and where there are comming from:
 ```scala
sbt> set logLevel := Level.Debug
sbt> protocUnpackDependencies
 ```
