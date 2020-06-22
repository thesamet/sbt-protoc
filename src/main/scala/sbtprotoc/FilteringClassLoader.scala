package sbtprotoc

final class FilteringClassLoader(parent: ClassLoader, extraParentPrefixes: Seq[String] = Seq.empty)
    extends ClassLoader(parent: ClassLoader) {
  private val parentPrefixes = List(
    "java.",
    "scala.",
    "sun.reflect.",
    "jdk.internal.reflect."
  ) ++ extraParentPrefixes

  override def loadClass(name: String, resolve: Boolean): Class[_] = {
    if (parentPrefixes.exists(name.startsWith _)) {
      super.loadClass(name, resolve)
    } else {
      null
    }
  }
}
