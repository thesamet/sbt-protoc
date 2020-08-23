# Changelog

## [1.0.0-RC1]
* Dependency on protoc-jar has been removed. By default, the dependency on a protoc executable is provided
  through SBT's dependency management. Read below.
* New task key: `protocExecutable: TaskKey[File]` returns a path to a protoc executable.
  By default this task downloads this file from the maven location specified by `protocDependency`.
  You can override this task to return a locally installed protoc.
* New setting: `protocDependency: SettingKey[ModuleID]` - provides a maven artifact to be downloaded
  by `protocExecutable`. Defaults to a protoc version matched by `PB.protocVersion`.
* `PB.protocVersion` used to have a `-v` prefix. This is no longer required and a deprecation warning is issued.
* `runProtoc` has been changed to a `TaskKey[Seq[String] => Int]`. The default implementation runs `protocExecutable`
  with the provided arguments and returns it exit code. SBT's logging facilities are provided to the protoc process.
* NixOS workarounds: if the environment variable `NIX_CC` is present, it is used to locate a dynamic linker (by reading `$NIX_CC/nix-support/dynamic-linker`). The located dynamic linker is used to run `protoc.exe` as well as downloaded native plugins, for seamless development experience in nix-shell. See #505.
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

