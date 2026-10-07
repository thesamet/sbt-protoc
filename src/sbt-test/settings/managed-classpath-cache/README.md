Regression test for https://github.com/thesamet/sbt-protoc/issues/463.

Run from the repository root:

```sh
sbt '++ 3.x' 'scripted settings/managed-classpath-cache'
sbt 'scripted settings/managed-classpath-cache'
```

The fixture supplies a synthetic update report containing a compile-only artifact
and one artifact for each protobuf configuration. It uses the plugin's actual
`managedClasspath` tasks and checks their selected artifacts.

After warming both tasks, it resets a counter and asserts that another invocation
does not access the unrelated artifact. The probe counts `File.toPath` calls,
which sjson-new's file codec makes before hashing file contents. It delegates to
the real path, so the normal hashing and cache lookup still take place.

On the affected sbt 2 implementation, the assertion reports two accesses even
though sbt reports two disk cache hits. Wrapping both task bodies in
`Def.uncached` eliminates those accesses. sbt 1 should pass without a fix.

This is a deterministic assertion test, not a benchmark: it needs no large
dependencies, permission changes, sleeps, or elapsed-time threshold. It detects
unnecessary report traversal; it does not measure the reported wall-clock cost.
