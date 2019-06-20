package sbtprotoc
import java.{util => ju}

object SystemDetector {
  private val UNKNOWN   = "unknown"
  private val X86_64_RE = "^(x8664|amd64|ia32e|em64t|x64)$".r
  private val X86_32_RE = "^(x8632|x86|i[3-6]86|ia32|x32)$".r

  def normalizedOs(s: String): String = normalize(s) match {
    case p if p.startsWith("linux")                         => "linux"
    case p if p.startsWith("osx") || p.startsWith("macosx") => "osx"
    case p if p.startsWith("windows")                       => "windows"
    case p if p.startsWith("freebsd")                       => "freebsd"
    case p if p.startsWith("openbsd")                       => "openbsd"
    case p if p.startsWith("netbsd")                        => "netbsd"
    case _                                                  => UNKNOWN
  }

  def normalizedArch(s: String): String = normalize(s) match {
    case X86_64_RE(_) => "x86_64"
    case X86_32_RE(_) => "x86_32"
    case _            => UNKNOWN
  }

  def detectedClassifier(): String = {
    val osName = sys.props.getOrElse("os.name", "")
    val osArch = sys.props.getOrElse("os.arch", "")
    System.getProperty("os.name")
    normalizedOs(osName) + "-" + normalizedArch(osArch)
  }

  def normalize(s: String) =
    s.toLowerCase(ju.Locale.US).replaceAll("[^a-z0-9]+", "")
}
