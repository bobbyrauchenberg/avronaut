package unit

import common._
import com.rauchenberg.cupcatAvro.schema.annotations.SchemaAnnotations.{Doc, Name, Namespace, SchemaMetadata}
import common.UnitSpecBase
import Records._
import com.rauchenberg.cupcatAvro.schema.instances

class SchemaAnnotationsSpec extends UnitSpecBase with instances {

  "annotations" should {

    "amend the record name if present" in {
      val expected = s"""{"type":"record","name":"CupcatName","doc":"","fields":[{"name":"cuppers","type":"string","doc":""}]}"""
      schemaAsString[CupcatName] should beRight(expected)
    }

    "add documentation if present" in {
      val expected = s"""{"type":"record","name":"CupcatDoc","doc":"","fields":[{"name":"cupcat","type":"string","doc":\"Field now deprecated"}]}"""
      schemaAsString[CupcatDoc] should beRight(expected)
    }

    "add global namespace if present" in {
      val expected = s"""{"type":"record","name":"CupcatNamespace","namespace":"com.cupcat","doc":"","fields":[{"name":"cupcat","type":"string","doc":""}]}"""
      schemaAsString[CupcatNamespace] should beRight(expected)
    }

  }

}

private [this] object Records {
  case class CupcatName(@SchemaMetadata(Map(Name -> "cuppers")) cupcat: String)
  case class CupcatDoc(@SchemaMetadata(Map(Doc -> "Field now deprecated")) cupcat: String)

  @SchemaMetadata(Map(Namespace -> "com.cupcat"))
  case class CupcatNamespace(cupcat: String)
}
