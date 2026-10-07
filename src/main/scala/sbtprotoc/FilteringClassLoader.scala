package sbtprotoc

final class FilteringClassLoader(parent: ClassLoader, extraParentPrefixes: Seq[String] = Seq.empty)
    extends ClassLoader(parent: ClassLoader) {
  private val parentPrefixes = List(
    "java.",
    "scala.",
    "sun.reflect.",
    "jdk.internal.reflect."
  ) ++ extraParentPrefixes

  // The topmost non-bootstrap loader is the platform loader on Java 9+ and
  // the extension loader on Java 8. Use Java 8 APIs to retain compatibility,
  // and start above the system loader to keep application dependencies hidden.
  private val platformClassLoader = {
    @annotation.tailrec
    def rootLoader(loader: ClassLoader): ClassLoader =
      if (loader == null || loader.getParent == null) loader
      else rootLoader(loader.getParent)

    rootLoader(ClassLoader.getSystemClassLoader.getParent)
  }

  override def loadClass(name: String, resolve: Boolean): Class[?] = {
    if (parentPrefixes.exists(name.startsWith)) {
      super.loadClass(name, resolve)
    } else {
      try Class.forName(name, false, platformClassLoader)
      catch {
        case _: ClassNotFoundException => null
      }
    }
  }
}
