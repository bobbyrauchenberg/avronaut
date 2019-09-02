package unit

import common._
import com.rauchenberg.cupcatAvro.schema.annotations.SchemaAnnotations.{Doc, Name, Namespace, SchemaMetadata}
import common.UnitSpecBase
import Records._
import com.rauchenberg.cupcatAvro.schema.AvroSchema

class AnnotationsSpec extends UnitSpecBase {

  "annotations" should {

    "amend the field name if name annotation is present" in new TestContext {
      runAssert[FieldNameAnnotation](s"""{"type":"record","name":"FieldNameAnnotation","namespace":"unit.Records","doc":"","fields":[{"name":"cuppers","type":"string","doc":""}]}""")
    }

    "add documentation if present" in new TestContext {
      runAssert[FieldDocAnnotation](s"""{"type":"record","name":"FieldDocAnnotation","namespace":"unit.Records","doc":"","fields":[{"name":"cupcat","type":"string","doc":\"Field now deprecated"}]}""")
    }

    "add global namespace if present" in new TestContext {
      runAssert[NamespaceAnnotation](
        s"""{"type":"record","name":"NamespaceAnnotation","namespace":"com.cupcat","doc":"","fields":[{"name":"cupcat","type":"string","doc":""}]}"""
      )
    }

    "treat a name with a dot as a fullname, so namespace is ignored" in new TestContext {
      runAssert[FullName](
        s"""{"type":"record","name":"Cat","namespace":"cup","doc":"","fields":[{"name":"cupcat","type":"string","doc":""}]}"""
      )
    }

    "document a top level record " in new TestContext {
      runAssert[TopLevelDoc](
        s"""{"type":"record","name":"TopLevelDoc","namespace":"unit.Records","doc":"Record is now deprecated","fields":[{"name":"cupcat","type":"string","doc":""}]}"""
      )
    }

  }

  trait TestContext {
    def runAssert[T : AvroSchema](expected: String) = schemaAsString[T] should beRight(expected)
  }

}

private [this] object Records {
  case class FieldNameAnnotation(@SchemaMetadata(Map(Name -> "cuppers")) cupcat: String)
  case class FieldDocAnnotation(@SchemaMetadata(Map(Doc -> "Field now deprecated")) cupcat: String)

  @SchemaMetadata(Map(Namespace -> "com.cupcat"))
  case class NamespaceAnnotation(cupcat: String)

  @SchemaMetadata(Map(Name -> "cup.Cat", Namespace -> "com.cupcat"))
  case class FullName(cupcat: String)

  @SchemaMetadata(Map(Doc -> "Record is now deprecated"))
  case class TopLevelDoc(cupcat: String)
}
