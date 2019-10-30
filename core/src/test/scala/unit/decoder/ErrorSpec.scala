package unit.decoder

import com.rauchenberg.avronaut.common.Error
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.GenericData
import unit.utils.UnitSpecBase

class ErrorSpec extends UnitSpecBase {

  "Decoder" should {

    "be able to accumulate errors" in {
      val schema        = AvroSchema.toSchema[ManyFields]
      val decoder       = Decoder[ManyFields]
      val genericRecord = new GenericData.Record(schema.data.value.schema)

      val expected = List(
        Error("Decoding failed for param 'field1' with value '' from the GenericRecord"),
        Error("Decoding failed for param 'field2' with value '' from the GenericRecord"),
        Error("Decoding failed for param 'field3' with value '' from the GenericRecord"),
        Error("Decoding failed for param 'field4' with value '' from the GenericRecord"),
        Error("Decoding failed for param 'field5' with value '' from the GenericRecord"),
        Error(s"""The failing GenericRecord was: ${genericRecord.toString}""")
      )

      Decoder.decodeAccumlating[ManyFields](genericRecord, decoder) should beLeft(expected)
    }

    "be able to fail fast and return the first error" in {
      val schema        = AvroSchema.toSchema[ManyFields]
      val decoder       = Decoder[ManyFields]
      val genericRecord = new GenericData.Record(schema.data.value.schema)

      val expected = List(
        Error("Decoding failed for param 'field1' with value '' from the GenericRecord"),
        Error(s"""The failing GenericRecord was: ${genericRecord.toString}""")
      )

      Decoder.decode[ManyFields](genericRecord, decoder) should beLeft(expected)
    }

  }

  case class ManyFields(field1: Int, field2: String, field3: Boolean, field4: List[Int], field5: Double)

}
