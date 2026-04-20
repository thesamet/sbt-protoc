import sbt._
import java.util.concurrent.atomic.AtomicInteger

object ProtocCount {
  private val count          = new AtomicInteger(0)
  def incrementAndGet(): Int = count.incrementAndGet()
  def get(): Int             = count.get()
}
