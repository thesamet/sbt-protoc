# Changelog

## [Unreleased]

## [1.1.0-RC3]

Changes since 1.1.0-RC2.

### sbt and binary compatibility

* **The sbt 2 artifact now requires sbt 2.1.0 or newer.** It is built with
  Scala 3.9.0 LTS, up from sbt 2.0.0 / Scala 3.8.4 in RC2 (#484).
  sbt 2.0.x cannot consume this artifact: sbt and Scala 3 minor-version
  compatibility is backward, not forward, and an older compiler cannot read
  the newer TASTy. Upgrade `project/build.properties` to `sbt.version=2.1.0`
  or newer before using RC3 on sbt 2.
* The sbt 1 artifact still supports **sbt 1.9.9 or newer** and is built with
  Scala 2.12.21. The repository's own launcher is now sbt 1.13.1; this does
  not raise the sbt 1 consumer minimum.
* These Scala versions apply to plugins and build definitions, independently
  of the application's `scalaVersion`. Applications do not need to move to
  Scala 3.9 to use this plugin. Plugins depending on sbt-protoc must use the
  matching sbt/Scala artifact; sbt 2 plugins consuming RC3 need a Scala 3.9+
  compiler and an sbt 2.1+ baseline.
* For ScalaPB on sbt 2, use **compilerplugin 0.11.21 or a compatible newer
  release**. Earlier ScalaPB 0.11.x Scala 3 generators can fail with
  `NoSuchMethodError` for `$init$` because they link against Scala 2.13-encoded
  protoc-gen traits. ScalaPB 0.11.21 fixes this by using `protoc-gen_3`
  ([ScalaPB#2228](https://github.com/scalapb/ScalaPB/pull/2228)); it is tested
  here on both sbt 1 and sbt 2. Keep ScalaPB's generator and application
  runtime versions aligned.
* Upgrade `sbt2-compat` from 0.1.0 to 0.2.0 and `protoc-bridge` from 0.9.10
  to 0.10.0. A build combining older generators with protoc-bridge 0.10.0 may
  report an early-semver eviction conflict. For the tested ScalaPB 0.11.21
  combination, add the following to `project/plugins.sbt` if needed; this
  acknowledges that dependency pairing and does not fix a Scala 2/3 binary
  mismatch:

  ```scala
  libraryDependencySchemes +=
    "com.thesamet.scalapb" %% "protoc-bridge" % VersionScheme.Always
  ```

See [sbt's plugin compatibility guidance](https://www.scala-sbt.org/2.x/docs/en/reference/plugin.html)
and [Scala's compatibility guarantees](https://www.scala-lang.org/development/).

### Generation and caching

* Update default protoc from 3.21.7 to **3.25.9**. Default Java and Kotlin
  targets now select their corresponding protobuf runtimes at **3.25.9**
  through protoc-bridge. Explicit generator runtime selections remain
  honored. When overriding protoc or runtime versions, choose a compatible
  pair; changing `PB.protocVersion` does not automatically change an explicit
  runtime selection.
* Fix sandboxed generators' access to JDK platform classes while preserving
  isolation from the parent build's application dependencies.
* Avoid sbt 2 action-caching of protobuf managed classpaths, eliminating
  unnecessary hashing of unrelated dependency artifacts (#479).
* Add opt-in `PB.cacheStyle` setting for content-hash based input tracking
  (`PB.CacheStyle.ContentHash`), useful in CI environments where file
  timestamps are not preserved across jobs. Default remains
  `PB.CacheStyle.LastModified` — existing behaviour is unchanged.
* Breaking cache-format change: on-disk cache state is now split into
  per-mode subdirectories (`cacheFile/mtime/`, `cacheFile/hash/`), and the
  artifact stamp order is now deterministic (previously relied on unstable
  HashMap iteration). Upgrading users will see a one-time spurious recompile
  on the first build after the upgrade regardless of which `cacheStyle` is
  selected; subsequent builds cache normally. Switching modes invalidates
  the inactive mode's snapshots, since both modes share generated outputs.
  (#430, #431)

### Build and validation

* Upgrade scalafmt to 3.11.5 while preserving Scala 2 syntax in scripted
  fixtures (#480), and update GitHub Actions dependencies (#489).
* Validate sbt 1 consumers on 1.9.9, 1.10.7, 1.12.9, and 1.13.1 across Linux,
  macOS, and Windows; validate sbt 2.1.0 on Java 17, 21, and 25.
* Publishing runs only after the full compatibility CI matrix passes for
  the release commit.

## [1.1.0-RC1]
* Add support for sbt 2

## [1.0.7]
* Update default protoc to 3.21.7
* For nix-os: support executing statically linked protoc (for versions >=3.23.0)

## [1.0.6]
* Update default protoc to 3.19.2.

## [1.0.5]
* Add option `PB.manifestProcessing` (defaults to true) that allows you to
  opt-opt of the manifest processing feature of sbt-protoc that automatically
  adds option protos as a source file to protoc.

## [1.0.4]
* `PB.targets` directories are no longer added to `managedSourceDirectories` as the underlying bug in intellij which prompted that change has been fixed in IntelliJ 2021.1.1.

## [1.0.3]
  * This version is published to maven instead of bintray.

## [1.0.2]
* Reduce the amount of artifact resolution & stamping (#227)
* Honor `cacheArtifactResolution := false` (#226)
* Don't run protoc with only Scalapb-Options-Proto sources (#225)
* Add `PB.targets` directories to `managedSourceDirectories` so that IDEs are aware of the generated sources in them and can provide autocompletion etc.
* Update default version of protoc to 3.15.6

## [1.0.1]
* Compile protos brought by `ProtobufSrcConfig` only once per project (#219)
* Better cache invalidation (#217)
* A plugin downloaded from a maven repository and injected via `asProtocPlugin()` can now be used in several PB.targets (#220)
* Protos referenced in the `ScalaPB-Options-Proto` manifest attribute in JARs included as `ProtobufConfig` are now automatically added to `protoSources`, allowing seamless integration with third-party JARs containing code generated by ScalaPB and compiled with different options that the current project (see https://github.com/scalapb/scalapb-validate/issues/70)

## [1.0.0-RC7]
* don't inherit parent configs' values for targets/protocOptions (c9ed1c2)

## [1.0.0-RC5]
* `PB.runProtoc` has been changed to a `TaskKey[ProtocRunner]` which can customize how protoc is being invoked. The default implementation runs `PB.protocExecutable` which in turns downloads and caches protoc executable from Maven. The version can be controlled with `PB.protocVersion`. If you want to run a locally installed protoc, it is suggested to customize `PB.protocExecutable` directly. See also [ProtocRunner source](https://github.com/scalapb/protoc-bridge/blob/665309d2c05cc45752d3800592cf3f191c5f6a63/bridge/src/main/scala/protocbridge/ProtocRunner.scala).

## [1.0.0-RC4]
* Avoid unnecessary warnings by do not passing non-existant directories to protoc (#197)

## [1.0.0-RC3]
* protoc executable is now cached under the user cache directory. The path
  used is platform dependent:
  * Linux/BSD/SOLARIS: `$XDG_CACHE_HOME/protocbridge/v1` or `$HOME/.cache/protocbridge/v1`
  * Windows: `{FOLDERID_LocalApplicationData}/protocbridge/protocbridge/cache`
  * OS X: `$HOME/Library/Caches/com.thesamet.scalapb/protocbridge/protocbridge/v1`

  The value is computed using the [directories](https://github.com/dirs-dev/directories-jvm) library
  and can be overridden by setting `PROTOC_CACHE` environment variable or the
  `protoc.cache` system property. If both are set, then the environment
  variable takes precedence.

## [1.0.0-RC2]
* The `protobuf` source directory is now part of
  * `unmanagedSourceDirectories` so that your IDE will take it as a part of your project and
  * `unmanagedResourceDirectories` so that the `.proto` files will be packed into the resulting JAR.

## [1.0.0-RC1]
* Dependency on protoc-jar has been removed. By default, the dependency on a protoc executable is provided
  through SBT's dependency management. Read below.
* New task key: `protocExecutable: TaskKey[File]` returns a path to a protoc executable.
  By default this task downloads this file from the maven location specified by `protocDependency`.
  You can override this task to return a locally installed protoc.
* New setting: `protocDependency: SettingKey[ModuleID]` - provides a maven artifact to be downloaded
  by `protocExecutable`. Defaults to a protoc version matched by `PB.protocVersion`.
* `PB.protocVersion` used to have a `-v` prefix. This is no longer required and a deprecation warning is issued. The version number is expected to be in x.y.z format (protocjar accepted a format without dots which is no longer supported).
* `runProtoc` has been changed to a `TaskKey[Seq[String] => Int]`. The default implementation runs `protocExecutable`
  with the provided arguments and returns it exit code. SBT's logging facilities are provided to the protoc process. Update: In 1.0.0-RC5, `PB.runProtoc` signature had changed again. See changelog entry above.
* NixOS workarounds: if the environment variable `NIX_CC` is present (usally ormally provided by `nix-shell`), it is used to locate a dynamic linker (by reading `$NIX_CC/nix-support/dynamic-linker`). The located dynamic linker is used to run `protoc` as well as downloaded native plugins, for seamless development experience in nix-shell. See #505 and [shell.nix](https://github.com/thesamet/sbt-protoc/blob/master/shell.nix).
* Deprecated and ignored setting key pythonExe has been removed.

## [0.99.31]

### Changed
- In earlier versions, if you wanted to generate `.proto` files for the `Test` configuration (for protos under src/test/protobuf)
you had to include a line like `inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings)`.
This line is no longer necessary, and in fact cause an error if the protos in
Test depend on the Protos in compile.

## [0.99.30]
### Added
* Added support for generating descriptor sets.
* Fixed race condition when one project depends on unpacked dependencies from
  another project.

## [0.99.29]
### Added
- Add new configuration protobuf-src that adds the unpacked protos in protoSources
- Updated protoc-jar to 3.11.4

