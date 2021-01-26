sbt-protoc
==========
[![CI](https://github.com/thesamet/sbt-protoc/workflows/CI/badge.svg)](https://github.com/thesamet/sbt-protoc/actions?query=workflow%3ACI)

This plugin uses protoc to generate code from proto files. This SBT plugin is
meant to supersede
[sbt-protobuf](https://github.com/sbt/sbt-protobuf/) and
[sbt-scalapb](https://github.com/scalapb/sbt-scalapb).

Highlights
----------

1. Generates source code directly under `src_managed` by default (works
   better with IntelliJ)
1. Supports compiling protos in both `test` and `compile` configuration.
1. Supports JVM-based code generators. [Write your own custom code generator.](https://scalapb.github.io/docs/writing-plugins)
1. Straightforward: No `PB.protobufSettings`, packaged as auto-plugin.

Installation
------------

**Step 1: create `project/protoc.sbt` with:**

```scala
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.0")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.10"
```

**Step 2: add to `build.sbt`:**

If you only want to generate Java code:

```scala
Compile / PB.targets := Seq(
  PB.gens.java -> (Compile / sourceManaged).value
)
```

A version of `protobuf-java` is going to get added to the runtime
dependencies. To explicitly set this version you can write:

```scala
Compile / PB.targets := Seq(
  PB.gens.java("3.7.0") -> (Compile / sourceManaged).value
)
```

For ScalaPB:
```scala
Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value
)
```

To generate Java + Scala with Java conversions:
```scala
Compile / PB.targets := Seq(
  PB.gens.java -> (Compile / sourceManaged).value,
  scalapb.gen(javaConversions = true) -> (Compile / sourceManaged).value
)
```

To make standard google.protobuf types available to import:
```
libraryDependencies ++= Seq(
  "com.google.protobuf" % "protobuf-java" % "3.13.0" % "protobuf"
)
```
The following includes both standard google.protobuf types and ScalaPB:

```scala
libraryDependencies ++= Seq(
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
)
```

## Download plugins that are available on maven repository

To download an artifact and use it as a code generator plugin:

```scala
libraryDependencies += "io.grpc" % "protoc-gen-grpc-java" % "1.23.0" asProtocPlugin()

Compile / PB.targets := Seq(
  PB.gens.plugin("grpc-java") -> (Compile / sourceManaged).value,
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

    Compile / PB.targets := Seq(
      PB.gens.plugin(name="myplugin", path="/path/to/plugin") -> (Compile / sourceManaged).value / "js"
    )

If you need to pass parameters to the plugin, it can be done as follows:

    val grpcWebGen = PB.gens.plugin(
      name="grpc-web",
      path="/usr/local/bin/protoc-gen-grpc-web-1.0.7-linux-x86_64"
    )

    Compile / PB.targets := Seq(
      (grpcWebGen, Seq("mode=grpcwebtext")) -> (Compile / sourceManaged).value / "js"
    )

**Step 3: Put some protos in src/main/protobuf and compile**

Migration notes to 1.0.0
------------------------

These notes are for those migrating from sbt-protoc < 1.0.0. 

* `PB.protocVersion` now accepts a version in `x.y.z` format (for example, `3.13.0`). Previously,
  this key accepted protoc-jar style version flags such as `-v361`. Version 1.0.x of ScalaPB will
  strip out the `-v` part (a behavior that will be deprecated later), however
  it does not take version numbers without dot seperators.
* Use `PB.protocExecutable` to use a locally installed `protoc`. By default
  this key downloads and caches `protoc` from Maven.
* If you previously used protoc-jar's option `--include_std_types`, see
  Installation instructions above, and look for "To make standard google.protobuf types available to import"
* Use `PB.protocRun` to have more control on how sbt-protoc invokes protoc (By
  default, it run `PB.protocExecutable`.

See [CHANGELOG.md](https://github.com/thesamet/sbt-protoc/blob/master/CHANGELOG.md) for more details.

Additional options
------------------

The options below need to be scoped to either `Compile` or `Test` (if unsure,
you probably want `Compile`)

Example settings:
```scala
// Force the version for the protoc binary
PB.protocVersion := "3.11.4"

// Additional directories to search for imports:
Compile / PB.includePaths ++= Seq(file("/some/other/path"))

// Make protos from some Jar available to import.
libraryDependencies ++= Seq(
  "com.google.protobuf" % "protobuf-java" % "3.13.0" % "protobuf"
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
Compile / PB.protoSources := Seq(sourceDirectory.value / "somewhere")

// Additional options to pass to protoc:
Compile / PB.protocOptions := Seq("-xyz")

// Excluding some proto files:
PB.generate / excludeFilter := "test-*.proto"

// Rarely needed: override where proto files from library dependencies are
// extracted to:
Compile / PB.externalIncludePath := file("/tmp/foo")

// By default we generate into target/src_managed. To customize:
Compile / PB.targets := Seq(
  scalapb.gen() -> file("/some/other/dir")
)

// Use a locally provided protoc (in 1.x):
PB.protocExecutable := file("/path/to/protoc")

// For sbt-protoc < 1.0 only:
Compile / PB.runProtoc := (args => Process("/path/to/protoc", args)!)

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

    Test / PB.targets := Seq(
        PB.gens.java("3.11.4") -> (Test / sourceManaged).value
    )

If you want to have protos in some other configuration (not `Compile` or
`Test`), for example `IntegrationTest` you need to manually add the plugin
default settings in that configuration:

```scala
configs(IntegrationTest)

inConfig(IntegrationTest)(sbtprotoc.ProtocPlugin.protobufConfigSettings)

IntegrationTest / PB.targets := Seq(
    PB.gens.java("3.11.4") -> (IntegrationTest / sourceManaged).value
)
```

Debugging
---------

Show proto files extracted and where there are comming from:
 ```scala
sbt> set logLevel := Level.Debug
sbt> protocUnpackDependencies
 ```
