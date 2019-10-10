package unit.common

import cats.syntax.either._
import com.rauchenberg.avronaut.common.{Avro, Error}
import org.scalacheck.{Arbitrary, Gen}
import unit.utils.UnitSpecBase

class AvroSpec extends UnitSpecBase {

  implicit val anyArb: Arbitrary[Any] = Arbitrary(
    Gen.oneOf(
      List(true,
           false,
           1,
           1.0f,
           1.0d,
           "1",
           "cupcat".getBytes,
           NotAString("cupcat"),
           List("cupcat"),
           Option("cupcat"),
           "cupcat".asLeft,
           "cupcat".asRight)))

  "AvroType" should {
    "not blindly convert anything to a string" in {
      forAll { a: Any =>
        whenever(!a.isInstanceOf[String]) {
          Avro.toAvroString(a) should beLeft(Error(s"'$a' is not a String"))
        }
      }
    }
    "check it gets bytes" in {
      forAll { a: Any =>
        whenever(!a.isInstanceOf[Array[Byte]]) {
          Avro.toAvroBytes(a) should beLeft(Error(s"'$a' is not an Array[Byte]"))
        }
      }
    }
  }

  case class NotAString(field: String)

}
