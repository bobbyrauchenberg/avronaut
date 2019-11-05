package unit.encoder

import com.rauchenberg.avronaut.common.Error
import com.rauchenberg.avronaut.encoder.Encoder
import unit.encoder.DodgyEncoders._
import unit.utils.UnitSpecBase

class ErrorSpec extends UnitSpecBase {

  "encoder" should {

    "be able to accumulate errors" in {

      val record  = ManyFields(1, "2", true, List(123), 4D)
      val encoder = Encoder[ManyFields]

      val expected = List(
        Error(s"Encoding failed for param 'field1' with value '1'"),
        Error(s"Encoding failed for param 'field3' with value 'true', original message 'boolean blew up'"),
        Error("Encoding failed for param 'field4' with value 'List(123)'")
      )

      Encoder.encodeAccumulating[ManyFields](record, encoder) should beLeft(expected)
    }

    "fail fast by default" in {

      val record  = ManyFields(1, "2", true, List(123), 4D)
      val encoder = Encoder[ManyFields]

      val expected = List(
        Error(s"Encoding failed for param 'field1' with value '1'")
      )

      Encoder.encode[ManyFields](record, encoder) should beLeft(expected)
    }

    "fail fast for an error returned by a typeclass instance" in {
      val record  = SingleField(true)
      val encoder = Encoder[SingleField]

      val expected = List(
        Error(s"Encoding failed for param 'field' with value 'true', original message 'boolean blew up'"),
      )

      Encoder.encode[SingleField](record, encoder) should beLeft(expected)
    }

  }

  case class ManyFields(field1: Int, field2: String, field3: Boolean, field4: List[Int], field5: Double)
  case class SingleField(field: Boolean)

}
