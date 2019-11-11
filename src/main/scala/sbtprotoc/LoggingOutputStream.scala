package sbtprotoc

import java.io.{ByteArrayOutputStream, OutputStream}

import sbt.{Level, Logger}

/** Split an OutputStream into messages and feed them to a given logger at a specified level. Not thread-safe. */
class LoggingOutputStream(logger: Logger, level: Level.Value, separator: String)
    extends OutputStream {
  private val baos = new ByteArrayOutputStream {
    def maybeStripSuffix(suffix: Array[Byte]): Option[String] = {
      def endsWithSuffix: Boolean = count >= suffix.length && suffix.zipWithIndex.forall {
        case (b: Byte, i: Int) =>
          b == buf(count - separatorBytes.length + i)
      }

      if (endsWithSuffix) Some(new String(buf, 0, count - separatorBytes.length))
      else None
    }
  }

  private val separatorBytes = separator.getBytes
  require(separatorBytes.length > 0)

  override def write(b: Int): Unit = {
    baos.write(b)
    baos.maybeStripSuffix(separatorBytes).foreach { message =>
      logger.log(level, message)
      baos.reset()
    }
  }
}

object LoggingOutputStream {
  def apply(logger: Logger, level: Level.Value): OutputStream =
    new LoggingOutputStream(logger, level, System.lineSeparator)
}
