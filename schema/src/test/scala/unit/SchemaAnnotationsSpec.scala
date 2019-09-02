package unit

import common._
import com.rauchenberg.cupcatAvro.schema.annotations.SchemaAnnotations.{Doc, Name, Namespace, SchemaMetadata}
import common.UnitSpecBase
import Records._
import com.rauchenberg.cupcatAvro.schema.{AvroSchema, instances}

class SchemaAnnotationsSpec extends UnitSpecBase with instances {

  "annotations" should {

    "amend the record name if present" in new TestContext {
      runAssert[NameAnnotation](s"""{"type":"record","name":"NameAnnotation","doc":"","fields":[{"name":"cuppers","type":"string","doc":""}]}""")
    }

    "add documentation if present" in new TestContext {
      runAssert[DocAnnotation](s"""{"type":"record","name":"DocAnnotation","doc":"","fields":[{"name":"cupcat","type":"string","doc":\"Field now deprecated"}]}""")
    }

    "add global namespace if present" in new TestContext {
      runAssert[NamespaceAnnotation](
        s"""{"type":"record","name":"NamespaceAnnotation","namespace":"com.cupcat","doc":"","fields":[{"name":"cupcat","type":"string","doc":""}]}"""
      )
    }

    "a name with a dot is a fullname, so namespace is ignored" in new TestContext {
      runAssert[FullName](
        s"""{"type":"record","name":"Cat","namespace":"cup","doc":"","fields":[{"name":"cupcat","type":"string","doc":""}]}"""
      )
    }

  }

  trait TestContext {
    def runAssert[T : AvroSchema](expected: String) = schemaAsString[T] should beRight(expected)
  }

}

private [this] object Records {
  case class NameAnnotation(@SchemaMetadata(Map(Name -> "cuppers")) cupcat: String)
  case class DocAnnotation(@SchemaMetadata(Map(Doc -> "Field now deprecated")) cupcat: String)

  @SchemaMetadata(Map(Namespace -> "com.cupcat"))
  case class NamespaceAnnotation(cupcat: String)

  @SchemaMetadata(Map(Name -> "cup.Cat", Namespace -> "com.cupcat"))
  case class FullName(cupcat: String)
}
