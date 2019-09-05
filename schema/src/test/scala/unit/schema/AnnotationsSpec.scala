package unit.schema

import common._
import com.rauchenberg.cupcatAvro.schema.annotations.SchemaAnnotations.{Doc, Name, Namespace, SchemaMetadata}
import common.UnitSpecBase
import com.rauchenberg.cupcatAvro.schema.AvroSchema

class AnnotationsSpec extends UnitSpecBase {

  "annotations" should {

    "amend the field name if name annotation is present" in new TestContext {
      runAssert[FieldNameAnnotation](
        s"""{"type":"record","name":"FieldNameAnnotation","namespace":"unit.schema.AnnotationsSpec",
           |"doc":"","fields":[{"name":"cuppers","type":"string"}]}""".stripMargin.replace("\n",""))
    }

    "add documentation if present" in new TestContext {
      runAssert[FieldDocAnnotation](
        s"""{"type":"record","name":"FieldDocAnnotation","namespace":"unit.schema.AnnotationsSpec",
           |"doc":"","fields":[{"name":"cupcat","type":"string","doc":\"Field now deprecated"}]}""".stripMargin.replace("\n",""))
    }

    "add global namespace if present" in new TestContext {
      runAssert[NamespaceAnnotation](
        s"""{"type":"record","name":"NamespaceAnnotation","namespace":"com.cupcat","doc":"",
           |"fields":[{"name":"cupcat","type":"string"}]}""".stripMargin.replace("\n","")
      )
    }

    "treat a name with a dot as a fullname, so namespace is ignored" in new TestContext {
      runAssert[FullName](
        s"""{"type":"record","name":"Cat","namespace":"cup","doc":"","fields":[{"name":"cupcat","type":"string"}]}"""
      )
    }

    "document a top level record " in new TestContext {
      runAssert[TopLevelDoc](
        s"""{"type":"record","name":"TopLevelDoc","namespace":"unit.schema.AnnotationsSpec","doc":"Record is now deprecated",
           |"fields":[{"name":"cupcat","type":"string"}]}""".stripMargin.replace("\n","")
      )
    }

  }

  trait TestContext {
    def runAssert[T : AvroSchema](expected: String) = schemaAsString[T] should beRight(expected)
  }

  case class FieldNameAnnotation(@SchemaMetadata(Map(Name -> "cuppers")) cupcat: String)
  case class FieldDocAnnotation(@SchemaMetadata(Map(Doc -> "Field now deprecated")) cupcat: String)
  @SchemaMetadata(Map(Namespace -> "com.cupcat"))
  case class NamespaceAnnotation(cupcat: String)
  @SchemaMetadata(Map(Name -> "cup.Cat", Namespace -> "com.cupcat"))
  case class FullName(cupcat: String)
  @SchemaMetadata(Map(Doc -> "Record is now deprecated"))
  case class TopLevelDoc(cupcat: String)
}
