import sbt._

object ProtocCount {
  private var count = 0
  def incrementAndGet(): Int = {
    count += 1
    count
  }
  def get(): Int = count
}
