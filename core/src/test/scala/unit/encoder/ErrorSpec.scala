package unit.encoder

import com.rauchenberg.avronaut.Codec
import com.rauchenberg.avronaut.Codec._
import com.rauchenberg.avronaut.common.Error
import unit.encoder.DodgyEncoders._
import unit.utils.UnitSpecBase

class ErrorSpec extends UnitSpecBase {

  "encoder" should {

    "be able to accumulate errors" in {

      val record         = ManyFields(1, "2", true, List(123), 4D)
      implicit val codec = Codec[ManyFields]

      val expected = List(
        Error(s"Encoding failed for param 'field1' with value '1'"),
        Error(s"Encoding failed for param 'field3' with value 'true', original message 'boolean blew up'"),
        Error("Encoding failed for param 'field4' with value 'List(123)'")
      )

      record.encodeAccumulating should beLeft(expected)
    }

    "fail fast by default" in {

      val record         = ManyFields(1, "2", true, List(123), 4D)
      implicit val codec = Codec[ManyFields]

      val expected = List(
        Error(s"Encoding failed for param 'field1' with value '1', original message 'int blew up'")
      )

      record.encode should beLeft(expected)
    }

  }

  case class ManyFields(field1: Int, field2: String, field3: Boolean, field4: List[Int], field5: Double)
  case class SingleField(field: Boolean)

}
