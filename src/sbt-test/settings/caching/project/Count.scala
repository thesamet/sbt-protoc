import sbt.*

object Count {
  private var count          = 0
  def incrementAndGet(): Int = {
    count += 1
    count
  }
}
