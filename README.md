sbt-protoc
==========

This plugin uses protoc to generate code from proto files. This SBT plugin is 
meant supercede 
[sbt-protobuf](https://github.com/sbt/sbt-protobuf/) and
[sbt-scalapb](https://github.com/trueaccord/sbt-scalapb/).

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
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.10")

libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.0"
```

**Step 2: add to `build.sbt`:**

If you only want to generate Java:

```
PB.targets in Compile := Seq(
  PB.gens.java -> (sourceManaged in Compile).value
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
  "com.google.protobuf" % "protobuf-java" % "3.3.1" % "protobuf"
)

// Changing where to look for protos to compile (default src/main/protobuf):
PB.protoSources in Compile := Seq(sourceDirectory.value / "somewhere")

// Additional options to pass to protoc:
PB.protocOptions in Compile := Seq("-xyz")

// Excluding some proto files:
excludeFilter in PB.generate := "test-*.proto"

// When compiling in Windows, Python is used to bridge protoc and this JVM.
// To set the path for Python.exe:
PB.pythonExe := "/path/to/python.exe"

// Rarely needed: override where proto files from library dependencies are
// extracted to:
PB.externalIncludePaths := file("/tmp/foo")

// By default we generate into target/src_managed. To customize:
PB.targets in Compile := Seq(
  scalapb.gen() -> file("/some/other/dir")
)

// Use a locally provided protoc:
PB.runProtoc := (args => Process("/path/to/protoc", args)!)
```

