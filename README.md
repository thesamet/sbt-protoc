sbt-protoc
==========

[![Travis CI](https://travis-ci.org/thesamet/sbt-protoc.svg?branch=master)](https://travis-ci.org/thesamet/sbt-protoc) [![AppVeyor](https://ci.appveyor.com/api/projects/status/wl4evfm0l5smimer/branch/master?svg=true)](https://ci.appveyor.com/project/thesamet/sbt-protoc/branch/master)

This plugin uses protoc to generate code from proto files. This SBT plugin is
meant supercede
[sbt-protobuf](https://github.com/sbt/sbt-protobuf/) and
[sbt-scalapb](https://github.com/thesamet/sbt-scalapb/).

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

```
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.15")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.7.0"
```

**Step 2: add to `build.sbt`:**

If you only want to generate Java:

```
PB.targets in Compile := Seq(
  PB.gens.java -> (sourceManaged in Compile).value
)
```

A version of `protobuf-java` is going to get added to the runtime
dependencies. To explicitly set this version you can write:

```
PB.targets in Compile := Seq(
  PB.gens.java("3.6.0rc1") -> (sourceManaged in Compile).value
)
```

For ScalaPB:
```
PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)
```

To generate Java + Scala with Java conversions:
```
PB.targets in Compile := Seq(
  PB.gens.java -> (sourceManaged in Compile).value,
  scalapb.gen(javaConversions = true) -> (sourceManaged in Compile).value
)
```

**Step 3: Put some protos in src/main/protobuf and compile**

Additional options
------------------

The options below need to be scoped to either `Compile` or `Test` (if unsure,
you probably want `Compile`)

Example settings:
```
// Additional directories to search for imports:
PB.includePaths in Compile ++= Seq(file("/some/other/path"))

// Make protos from some Jar available to import.
libraryDependencies ++= Seq(
  "com.google.protobuf" % "protobuf-java" % "3.5.1" % "protobuf"
)

// Changing where to look for protos to compile (default src/main/protobuf):
PB.protoSources in Compile := Seq(sourceDirectory.value / "somewhere")

// Additional options to pass to protoc:
PB.protocOptions in Compile := Seq("-xyz")

// Excluding some proto files:
excludeFilter in PB.generate := "test-*.proto"

// Before version 0.99.15, when compiling in Windows, Python was used to bridge
// protoc and this JVM. To set the path for Python.exe:
// Note that this must be Python2 and not Python3.
// Since version 0.99.15 this option has no effect, and will be removed in a
// future version.
PB.pythonExe := "/path/to/python.exe"

// Rarely needed: override where proto files from library dependencies are
// extracted to:
PB.externalIncludePath := file("/tmp/foo")

// By default we generate into target/src_managed. To customize:
PB.targets in Compile := Seq(
  scalapb.gen() -> file("/some/other/dir")
)

// Use a locally provided protoc:
PB.runProtoc in Compile := (args => Process("/path/to/protoc", args)!)

// Prevents the plugin from adding libraryDependencies to your project
PB.additionalDependencies := Nil
```

The following setting is needed, if you want to generate .proto definitions
in the `Test` scope.

```
inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings)
```
