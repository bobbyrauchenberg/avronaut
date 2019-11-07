package unit.encoder

import com.rauchenberg.avronaut.encoder.Encoder
import unit.utils.UnitSpecBase

class EncoderAPISpec extends UnitSpecBase {

  "encoder api" should {
    "allow users to get a schema" in {
      val encoder = Encoder[Record]

      val expected =
        """{"type":"record","name":"Record","namespace":"unit.encoder.EncoderAPISpec","doc":"",
          |"fields":[{"name":"field1","type":"int"},{"name":"field2","type":"string"},
          |{"name":"field3","type":"boolean"}]}""".stripMargin.replaceAll("\n", "")

      Encoder.schema(encoder).map(_.toString) should beRight(expected)
    }
  }

  case class Record(field1: Int, field2: String, field3: Boolean)

}
