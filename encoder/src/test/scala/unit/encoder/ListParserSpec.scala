package unit.encoder

import com.rauchenberg.avronaut.common.{AvroArray, AvroNumLong, AvroNumber}
import com.rauchenberg.avronaut.encoder.ListParser
import com.rauchenberg.avronaut.schema.AvroSchema
import unit.common.UnitSpecBase

import scala.collection.JavaConverters._

class ListParserSpec extends UnitSpecBase {

  "list parser" should {
    "handle a basic list" in {

      val schema = AvroSchema[SimpleList].schema.value

      val arraySchema = schema.getFields.asScala(0).schema()

      val list = AvroArray(List(AvroNumber(AvroNumLong(123)), AvroNumber(AvroNumLong(456))))

      println(ListParser(arraySchema, list).parse)
      ListParser(arraySchema, list).parse.map(_.asScala.toList) should beRight(List(123, 456).asInstanceOf[List[Any]])
    }

    "handle a nested list" in {

      val schema = AvroSchema[NestedList].schema.value

      val arraySchema = schema.getFields.asScala(0).schema()

      val list =
        AvroArray(List(AvroArray(List(AvroNumber(AvroNumLong(123)))), AvroArray(List(AvroNumber(AvroNumLong(456))))))

      println(ListParser(arraySchema, list).parse)
    }

  }

  case class SimpleList(field: List[Long])
  case class NestedList(field: List[List[Long]])

  case class Nested(field: Int)
  case class ListWithCaseClass(field: List[Nested])

}
