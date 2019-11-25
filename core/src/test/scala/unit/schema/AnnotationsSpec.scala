package unit.schema

import com.rauchenberg.avronaut.common.annotations.SchemaAnnotations.{Doc, Name, Namespace, SchemaMetadata}
import com.rauchenberg.avronaut.schema.AvroSchema
import com.sksamuel.avro4s.AvroNamespace
import unit.utils.UnitSpecBase
import unit.common._

class AnnotationsSpec extends UnitSpecBase {

  "annotations" should {

    "amend the field name if name annotation is present" in new TestContext {
      implicit val schema = AvroSchema.toSchema[FieldNameAnnotation]
      runAssert[FieldNameAnnotation](
        s"""{"type":"record","name":"FieldNameAnnotation","namespace":"unit.schema.AnnotationsSpec",
           |"fields":[{"name":"cuppers","type":"string"}]}""".stripMargin.replace("\n", ""))
    }

    "add documentation if present" in new TestContext {
      implicit val schema = AvroSchema.toSchema[FieldDocAnnotation]
      runAssert[FieldDocAnnotation](
        s"""{"type":"record","name":"FieldDocAnnotation","namespace":"unit.schema.AnnotationsSpec",
           |"fields":[{"name":"cupcat","type":"string","doc":\"Field now deprecated"}]}""".stripMargin
          .replace("\n", ""))
    }

    "add global namespace if present" in new TestContext {
      implicit val schema = AvroSchema.toSchema[NamespaceAnnotation]
      runAssert[NamespaceAnnotation](
        s"""{"type":"record","name":"NamespaceAnnotation","namespace":"com.cupcat",
           |"fields":[{"name":"cupcat","type":"string"}]}""".stripMargin.replace("\n", "")
      )
    }

    "treat a name with a dot as a fullname, so namespace is ignored" in new TestContext {
      implicit val schema = AvroSchema.toSchema[FullName]
      runAssert[FullName](
        s"""{"type":"record","name":"Cat","namespace":"cup","fields":[{"name":"cupcat","type":"string"}]}"""
      )
    }

    "document a top level record " in new TestContext {
      implicit val schema = AvroSchema.toSchema[TopLevelDoc]
      runAssert[TopLevelDoc](
        s"""{"type":"record","name":"TopLevelDoc","namespace":"unit.schema.AnnotationsSpec","doc":"Record is now deprecated",
           |"fields":[{"name":"cupcat","type":"string"}]}""".stripMargin.replace("\n", "")
      )
    }

    "apply annotations to an enum" in new TestContext {
      implicit val schema = AvroSchema.toSchema[AnnotatedEnum]
      val expected =
        """{"type":"record","name":"AnnotatedEnum","namespace":"unit.schema",
                       |"fields":[{"name":"cupcat","type":{"type":"enum","name":"cupcat",
                       |"symbols":["AnnotatedCup","AnnotatedCat"]}}]}""".stripMargin.replace("\n", "")

      runAssert[AnnotatedEnum](expected)
    }

    "apply annotations to enum at case class field level" in new TestContext {
      implicit val schema = AvroSchema.toSchema[AnotherAnnotatedEnum]

      println(schemaAsString[AnotherAnnotatedEnum])

    }

    "apply annotations to records that are elements of unions" in new TestContext {
      implicit val schema = AvroSchema.toSchema[AnnotatedUnion].data.map(_.schema).value

      val expected = """{"type":"record","name":"AnnotatedUnion","namespace":"unit.schema.AnnotationsSpec","fields"
                       |:[{"name":"field","type":["null",{"type":"record","name":"InUnion","namespace":"com.cupcat",
                       |"fields":[{"name":"field","type":"boolean"}]}]}]}
                       |""".stripMargin.replaceAll("\n", "")

      schema shouldBe expected
    }
  }

  trait TestContext {
    def runAssert[A : AvroSchema](expected: String) = schemaAsString[A] shouldBe expected
  }

  case class FieldNameAnnotation(@SchemaMetadata(Map(Name -> "cuppers")) cupcat: String)
  case class FieldDocAnnotation(@SchemaMetadata(Map(Doc   -> "Field now deprecated")) cupcat: String)
  @SchemaMetadata(Map(Namespace -> "com.cupcat"))
  case class NamespaceAnnotation(cupcat: String)
  @SchemaMetadata(Map(Name -> "cup.Cat", Namespace -> "com.cupcat"))
  case class FullName(cupcat: String)
  @SchemaMetadata(Map(Doc -> "Record is now deprecated"))
  case class TopLevelDoc(cupcat: String)

  case class InUnion(field: Boolean)
  case class AnnotatedUnion(@SchemaMetadata(Map(Namespace -> "com.cupcat")) field: Option[InUnion])

}

@SchemaMetadata(Map(Name -> "cupcat"))
sealed trait Annotated
case object AnnotatedCup extends Annotated
case object AnnotatedCat extends Annotated
case class AnnotatedEnum(cupcat: Annotated)

sealed trait AnotherAnnotated
case object A extends AnotherAnnotated
case object B extends AnotherAnnotated
case class AnotherAnnotatedEnum(s: String,
                                @AvroNamespace("com.rauchenberg")
                                @SchemaMetadata(Map(Namespace -> "com.rauchenberg")) enum: AnotherAnnotated)
