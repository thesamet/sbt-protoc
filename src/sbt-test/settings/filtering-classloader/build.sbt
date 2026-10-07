import java.net.URLClassLoader
import sbtprotoc.FilteringClassLoader

val checkClassLoading = taskKey[Unit]("Check platform visibility and dependency isolation")

checkClassLoading := {
  val parentOnly = classOf[javax.sbtprotoc.ParentOnly]
  val parent     = parentOnly.getClassLoader
  val filter     = new FilteringClassLoader(parent)
  val loader     = new URLClassLoader(Array.empty[java.net.URL], filter)
  try {
    Seq(
      classOf[java.sql.Connection],
      classOf[javax.sql.DataSource],
      classOf[javax.xml.parsers.DocumentBuilder],
      classOf[org.w3c.dom.Node],
      classOf[scala.Option[Any]]
    ).foreach { expected =>
      assert(loader.loadClass(expected.getName) == expected, expected.getName)
    }

    // A javax.* dependency in the build must not become visible just because
    // platform classes in that namespace are now accessible.
    Seq(parentOnly.getName, classOf[FilteringClassLoader].getName).foreach { name =>
      val hidden = try {
        loader.loadClass(name)
        false
      } catch {
        case _: ClassNotFoundException => true
      }
      assert(hidden, s"Build dependency leaked into the sandbox: $name")
    }
  } finally loader.close()

  val isolated = new URLClassLoader(
    Array(parentOnly.getProtectionDomain.getCodeSource.getLocation),
    filter
  )
  try {
    val loaded = isolated.loadClass(parentOnly.getName)
    assert(loaded.getClassLoader == isolated)
    assert(loaded != parentOnly)
  } finally isolated.close()

  val shared = new FilteringClassLoader(parent, Seq("javax.sbtprotoc."))
  assert(shared.loadClass(parentOnly.getName) == parentOnly)
}
