package unit

import com.rauchenberg.cupcatAvro.schema.AvroSchema
import common.UnitSpecBase
import org.apache.avro.Schema
import cats.syntax.option._
import com.rauchenberg.cupcatAvro.schema.helpers.SchemaHelper._
import SchemaHelper.UnionWithDefault

class SchemaHelperSpec extends UnitSpecBase {

  "moveDefaultToHead" should {
    "reorder the schema in a union with a default, so the type of the default value comes first" in {
      val schema = AvroSchema[UnionWithDefault].schema.right.get.getField("cupcat").schema
      moveDefaultToHead(schema, "cupcat".some, Option(Schema.Type.STRING)).map(_.toString) should beRight("""["string","null"]""")
    }
  }


}

private [this] object SchemaHelper {
  case class UnionWithDefault(cupcat: Option[String] = "cupcat".some, cuppers: Int)
}