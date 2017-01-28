import scala.scalajs.js.JSApp

object Main extends JSApp {
  def main(): Unit = {
    println(myproto.test.Test(foo = Some(13)).foo)
  }
}
