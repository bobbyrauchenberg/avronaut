package unit.codec

import com.rauchenberg.avronaut.Codec
import unit.utils.UnitSpecBase
import unit.common.schemaAsString

class CodecAPISpec extends UnitSpecBase {

  "codec api" should {
    "make a schema available" in {
      implicit val codec = Codec[Record]

      val expected =
        """{"type":"record","name":"Record","namespace":"unit.codec.CodecAPISpec",
          |"fields":[{"name":"field1","type":"int"},
          |{"name":"field2","type":"boolean"}]}""".stripMargin.replaceAll("\n", "")

      Codec.schema[Record].map { schema =>
        schema.toString shouldBe expected
      }

    }
  }

  case class Record(field1: Int, field2: Boolean)

}
