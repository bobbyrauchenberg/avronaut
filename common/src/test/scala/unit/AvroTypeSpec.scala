package unit

import cats.syntax.either._
import com.rauchenberg.avronaut.common.{AvroType, Error}
import org.scalacheck.{Arbitrary, Gen}

class AvroTypeSpec extends UnitSpecBase {

  implicit val anyArb: Arbitrary[Any] = Arbitrary(
    Gen.oneOf(
      List(true,
           false,
           1,
           1.0f,
           1.0d,
           NotAString("cupcat"),
           List("cupcat"),
           Option("cupcat"),
           "cupcat".asLeft,
           "cupcat".asRight)))

  "AvroType" should {
    "not blindly convert anything to a string" in {
      forAll { a: Any =>
        whenever(!a.isInstanceOf[String]) {
          AvroType.toAvroString(a) should beLeft(Error(s"tried to create a string from $a"))
        }
      }
    }
  }

  case class NotAString(field: String)

}
