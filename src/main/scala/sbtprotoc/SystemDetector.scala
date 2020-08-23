package sbtprotoc

object SystemDetector {
  @deprecated("Use protocbridge.SystemDetector.normalizedOs instead", "1.0.0")
  def normalizedOs(s: String): String = protocbridge.SystemDetector.normalizedOs(s)

  @deprecated("Use protocbridge.SystemDetector.normalizedArch instead", "1.0.0")
  def normalizedArch(s: String): String = protocbridge.SystemDetector.normalizedArch(s)

  @deprecated("Use protocbridge.SystemDetector.detectedClassifier() instead", "1.0.0")
  def detectedClassifier(): String = protocbridge.SystemDetector.detectedClassifier()

  @deprecated("Use protocbridge.SystemDetector.normalize instead", "1.0.0")
  def normalize(s: String) = protocbridge.SystemDetector.normalize(s)
}
