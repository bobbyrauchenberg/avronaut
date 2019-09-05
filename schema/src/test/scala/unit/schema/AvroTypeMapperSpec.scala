package unit.schema

import cats.syntax.option._
import common.UnitSpecBase
import com.rauchenberg.cupcatAvro.schema.helpers.AvroTypeMapper._
import org.apache.avro.Schema

class AvroTypeMapperSpec extends UnitSpecBase {

  "schemaFor" should {
    "return the schema type for a value" in {
      avroTypeFor("cupcat") shouldBe Schema.Type.STRING.some
      avroTypeFor(123) shouldBe Schema.Type.INT.some
      avroTypeFor(123L) shouldBe Schema.Type.LONG.some
      avroTypeFor(123f) shouldBe Schema.Type.FLOAT.some
      avroTypeFor(123d) shouldBe Schema.Type.DOUBLE.some
      avroTypeFor("cupcat".getBytes) shouldBe Schema.Type.BYTES.some
      avroTypeFor(List("cupcat")) shouldBe Schema.Type.ARRAY.some
      avroTypeFor(Seq("cupcat")) shouldBe Schema.Type.ARRAY.some
      avroTypeFor(Vector("cupcat")) shouldBe Schema.Type.ARRAY.some
      avroTypeFor(Map("cup" -> "cat")) shouldBe Schema.Type.MAP.some
    }

    "return the schema type for the value in an option" in {
      avroTypeFor(Option("cupcat")) shouldBe Schema.Type.STRING.some
      avroTypeFor(Option(123)) shouldBe Schema.Type.INT.some
      avroTypeFor(Option(123L)) shouldBe Schema.Type.LONG.some
      avroTypeFor(Option(123f)) shouldBe Schema.Type.FLOAT.some
      avroTypeFor(Option(123d)) shouldBe Schema.Type.DOUBLE.some
      avroTypeFor(Option("cupcat".getBytes)) shouldBe Schema.Type.BYTES.some
      avroTypeFor(Option(List("cupcat"))) shouldBe Schema.Type.ARRAY.some
      avroTypeFor(Option(Seq("cupcat"))) shouldBe Schema.Type.ARRAY.some
      avroTypeFor(Option(Vector("cupcat"))) shouldBe Schema.Type.ARRAY.some
      avroTypeFor(Option(Map("cup" -> "cat"))) shouldBe Schema.Type.MAP.some
    }

    "return schema type Null for None" in {
      avroTypeFor(None) shouldBe Schema.Type.NULL.some
    }

  }

}

private[this] object AvroHelper {
  case class Blah(cupcat: String)
}