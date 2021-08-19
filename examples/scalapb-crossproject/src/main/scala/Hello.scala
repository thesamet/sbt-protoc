package example

import myproto.test.Test

object Hello {
  def main(args: Array[String]): Unit = {
    println(Test(foo = Some(32)).toByteArray.toVector)
  }
}
