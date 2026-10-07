package repro

object Check extends App {
  val original = smoke.Smoke(message = "ScalaPB generator smoke test")
  assert(smoke.Smoke.parseFrom(original.toByteArray) == original)
}
