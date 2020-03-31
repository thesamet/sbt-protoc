# Changelog

## [Unreleased]

## [0.99.31]

### Changed
- In earlier versions, if you wanted to generate `.proto` files for the `Test` configuration (for protos under src/test/protobuf)
you had to include a line like `inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings)`.
This line is no longer necessary, and in fact cause an error if the protos in
Test depend on the Protos in compile.

## [0.99.30]
### Added
- Added support for generating descriptor sets.
- Fixed race condition when one project depends on unpacked dependencies from
  another project.

## [0.99.29]
### Added
- Add new configuration protobuf-src that adds the unpacked protos in protoSources
- Updated protoc-jar to 3.11.4

