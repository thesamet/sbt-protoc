import myproto.test.TestMessage

@main def hello() = {
  val x = TestMessage(foo = Some(3))
  println("Welcome!")
  println(x)
  println(x.serializedSize)
  println(x.toProtoString)
}
