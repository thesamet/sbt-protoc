import sbt._

object TestState {
  private var protocCount = 0

  def incrementProtocAndGet(): Int = {
    protocCount += 1
    protocCount
  }

  def protocInvocations(): Int = protocCount
}
