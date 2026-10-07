import java.io.File
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

object HashProbe {
  val accesses = new AtomicInteger()

  // sjson-new's File hash codec calls toPath before hashing the file contents.
  // This file belongs only to the unrelated configuration, so classpath selection
  // has no reason to access its path. Count accesses without preventing real hashing.
  def file(path: File): File = new File(path.getPath) {
    override def toPath: Path = {
      accesses.incrementAndGet()
      super.toPath
    }
  }
}
