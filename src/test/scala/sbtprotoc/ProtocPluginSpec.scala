package sbtprotoc

import org.scalatest._
import org.scalacheck._
import org.scalatest.prop.GeneratorDrivenPropertyChecks

object Files {
  sealed trait Entry
  case class Directory(entries: Map[String, Entry]) extends Entry
  case class File(contents: String) extends Entry

  def merge(dir1: Directory, dir2: Directory): Directory = {
    val r: Map[String, Entry] = for {(name, entry) <- dir2.entries} yield {
      (entry, dir1.entries.get(name)) match {
        case (_, None) => (name, entry)
        case (d1: Directory, Some(d2: Directory)) => (name, merge(d1, d2))
        case _ => throw new RuntimeException("Can't merge different types.")
      }
    }
    Directory(dir1.entries ++ r)
  }

  def genFile = Gen.alphaStr.map(File)

  def genDirectory(depth: Int = 0): Gen[Entry] = for {
    names <- Gen.listOf(Gen.alphaStr).map(_.distinct)
    entries <- Gen.listOfN(names.size, genEntry(depth + 1))
  } yield Directory((names zip entries).toMap)

  def genEntry(depth: Int): Gen[Entry] = if (depth < 3)
    Gen.oneOf(genFile, genDirectory(depth))
  else genFile
}

class ProtocPluginSpec extends FlatSpec with MustMatchers with GeneratorDrivenPropertyChecks {
  "moveDir" should "work as expected" in {
    forAll(Files.genDirectory()) {
      d =>
        true must be(true)
    }
  }
}
