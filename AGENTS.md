# Plugin compatibility policy

This repository publishes an sbt plugin. Its compiler versions are constrained by
the Scala compiler used by the minimum supported sbt, independently of the Scala
version of a user's application.

- Build the sbt 1 artifact with Scala 2.12 (currently 2.12.21). Do not upgrade it
  to Scala 2.13. Keep examples that intentionally cover Scala 2.12 on that line.
- Build the sbt 2 artifact with Scala 3.8 (currently 3.8.4). Do not automatically
  upgrade to Scala 3.9 or another minor line: an older Scala compiler is not
  guaranteed to consume TASTy emitted by a newer minor compiler.
- Keep the repository's build launcher (`project/build.properties`) on sbt 1.x
  while using the existing cross-build to publish sbt 1 and sbt 2 artifacts.
  Moving the launcher to sbt 2 requires a deliberate build migration.
- `pluginCrossBuild / sbtVersion` declares the minimum supported sbt versions:
  currently 1.9.9 and 2.0.0. The `sbt2` value in `build.sbt` also selects the sbt 2
  scripted-test version. Do not raise these baselines just to fix a dependency
  update's CI failures.

Patch updates within the supported compiler lines may be considered, but must
pass the cross-build and scripted tests. To change a compiler line or minimum sbt
version, explicitly establish the new compatibility floor, document the change,
and verify that consuming builds load and use the plugin on that minimum version.
Test newer sbt releases separately without silently raising the publishing floor.

Keep `.scala-steward.conf` and the `scala-steward:off` markers in `build.sbt`
aligned with this policy. Steward scans only the root plugin build (`buildRoots =
["."]`). Its pins apply to every configured build root. Existing examples and
scripted fixtures also cover Scala 2.13; preserve that coverage. Before adding
those directories to Steward's build roots, revise the configuration so their
compiler updates are not constrained to the plugin's Scala 2.12 line.

Relevant checks:

```sh
sbt '+ test' '+ scalafmtCheckAll' scalafmtSbtCheck
sbt 'set scriptedSbt := "1.9.9"' scripted
sbt '++ 3.x' scriptedTestSbt2
```

References:

- [sbt plugin compiler and minimum-version guidance](https://www.scala-sbt.org/2.x/docs/en/reference/plugin.html)
- [Scala compatibility guarantees](https://www.scala-lang.org/development/)
- [Scala Steward repository configuration](https://github.com/scala-steward-org/scala-steward/blob/main/docs/repo-specific-configuration.md)
