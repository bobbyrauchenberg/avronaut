package unit

import cats.syntax.option._
import com.rauchenberg.cupcatAvro.schema.AvroSchema
import common.UnitSpecBase
import com.rauchenberg.cupcatAvro.schema.SchemaHelper._
import org.apache.avro.Schema
import SchemaHelperClasses.UnionWithDefault

class SchemaHelperSpec extends UnitSpecBase {

  "schemaFor" should {
    "return the schema type for a value" in {
      schemaFor("cupcat") shouldBe Schema.Type.STRING.some
      schemaFor(123) shouldBe Schema.Type.INT.some
      schemaFor(123L) shouldBe Schema.Type.LONG.some
      schemaFor(123f) shouldBe Schema.Type.FLOAT.some
      schemaFor(123d) shouldBe Schema.Type.DOUBLE.some
      schemaFor("cupcat".getBytes) shouldBe Schema.Type.BYTES.some
      schemaFor(List("cupcat")) shouldBe Schema.Type.ARRAY.some
      schemaFor(Seq("cupcat")) shouldBe Schema.Type.ARRAY.some
      schemaFor(Vector("cupcat")) shouldBe Schema.Type.ARRAY.some
      schemaFor(Map("cup" -> "cat")) shouldBe Schema.Type.MAP.some
    }

    "return the schema type for the value in an option" in {
      schemaFor(Option("cupcat")) shouldBe Schema.Type.STRING.some
      schemaFor(Option(123)) shouldBe Schema.Type.INT.some
      schemaFor(Option(123L)) shouldBe Schema.Type.LONG.some
      schemaFor(Option(123f)) shouldBe Schema.Type.FLOAT.some
      schemaFor(Option(123d)) shouldBe Schema.Type.DOUBLE.some
      schemaFor(Option("cupcat".getBytes)) shouldBe Schema.Type.BYTES.some
      schemaFor(Option(List("cupcat"))) shouldBe Schema.Type.ARRAY.some
      schemaFor(Option(Seq("cupcat"))) shouldBe Schema.Type.ARRAY.some
      schemaFor(Option(Vector("cupcat"))) shouldBe Schema.Type.ARRAY.some
      schemaFor(Option(Map("cup" -> "cat"))) shouldBe Schema.Type.MAP.some
    }

    "return schema type Null for None" in {
      schemaFor(None) shouldBe Schema.Type.NULL.some
    }

  }

  "moveDefaultToHead" should {
    "reorder the schema in a union with a default, so the type of the default value comes first" in {
        val schema = AvroSchema[UnionWithDefault].schema.right.get.getField("cupcat").schema
        moveDefaultToHead(schema, "cupcat".some, Option(Schema.Type.STRING)).map(_.toString) should beRight("""["string","null"]""")
    }
  }

}

private[this] object SchemaHelperClasses {
  case class Blah(cupcat: String)
  case class UnionWithDefault(cupcat: Option[String] = "cupcat".some, blah: Blah)
}