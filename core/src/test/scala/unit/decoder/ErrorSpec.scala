package unit.decoder

import com.rauchenberg.avronaut.Codec
import com.rauchenberg.avronaut.Codec._
import com.rauchenberg.avronaut.common.Error
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.GenericData
import unit.utils.UnitSpecBase

class ErrorSpec extends UnitSpecBase {

  "Decoder" should {

    "be able to accumulate errors" in {
      val schema         = AvroSchema.toSchema[ManyFields]
      implicit val codec = Codec[ManyFields]
      val genericRecord  = new GenericData.Record(schema.data.value.schema)

      val expected = List(
        Error("Decoding failed for param 'field1' with value 'null' from the GenericRecord"),
        Error("Decoding failed for param 'field2' with value 'null' from the GenericRecord"),
        Error("Decoding failed for param 'field3' with value 'null' from the GenericRecord"),
        Error("Decoding failed for param 'field4' with value 'null' from the GenericRecord"),
        Error("Decoding failed for param 'field5' with value 'null' from the GenericRecord"),
        Error(s"The value passed to the record decoder was: ${genericRecord.toString}")
      )
      genericRecord.decodeAccumulating[ManyFields] should beLeft(expected)
    }

    "be able to fail fast and return the first error" in {
      val schema         = AvroSchema.toSchema[ManyFields]
      implicit val codec = Codec[ManyFields]
      val genericRecord  = new GenericData.Record(schema.data.value.schema)

      val expected = List(
        Error(s"Decoding failed for param 'field1' with value 'null' from the GenericRecord"),
        Error(s"The value passed to the record decoder was: ${genericRecord.toString}")
      )

      genericRecord.decode[ManyFields] should beLeft(expected)
    }

  }

  case class ManyFields(field1: Int, field2: String, field3: Boolean, field4: List[Int], field5: Double)

}
