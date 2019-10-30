package unit.encoder

import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import unit.utils.UnitSpecBase
import DodgyEncoders._
import com.rauchenberg.avronaut.common.Error

class ErrorSpec extends UnitSpecBase {

  "encoder" should {

    "accumulate errors when encodeAccumulating method is called" in {

      val record = ManyFields(1, "2", true, List(123), 4D)
      val schema = AvroSchema.toSchema[ManyFields]

      val encoder = Encoder[ManyFields]

      val expected = List(
        Error(s"Encoding failed for param 'field1' with value '1'"),
        Error(s"Encoding failed for param 'field3' with value 'true', original message 'boolean blew up'"),
        Error("Encoding failed for param 'field4' with value 'List(123)'")
      )

      Encoder.encodeAccumulating[ManyFields](record, encoder, schema.data) should beLeft(expected)
    }

    "fail fast by default" in {

      val record = ManyFields(1, "2", true, List(123), 4D)
      val schema = AvroSchema.toSchema[ManyFields]

      val encoder = Encoder[ManyFields]

      val expected = List(
        Error(s"Encoding failed for param 'field1' with value '1'")
      )

      Encoder.encode[ManyFields](record, encoder, schema.data) should beLeft(expected)
    }

    "fail fast for an error returned by a typeclass instance" in {
      val record = SingleField(true)
      val schema = AvroSchema.toSchema[SingleField]

      val encoder = Encoder[SingleField]

      val expected = List(
        Error(s"Encoding failed for param 'field' with value 'true', original message 'boolean blew up'"),
      )

      Encoder.encode[SingleField](record, encoder, schema.data) should beLeft(expected)
    }

  }

  case class ManyFields(field1: Int, field2: String, field3: Boolean, field4: List[Int], field5: Double)
  case class SingleField(field: Boolean)

}
