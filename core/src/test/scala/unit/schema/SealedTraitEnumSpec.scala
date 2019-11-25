package unit.schema

import com.rauchenberg.avronaut.common.annotations.SchemaAnnotations.{Name, Namespace, SchemaMetadata}
import com.rauchenberg.avronaut.schema.AvroSchema
import unit.utils.UnitSpecBase
import unit.schema.SealedTraitEnum._
import unit.common._

class SealedTraitEnumSpec extends UnitSpecBase {

  "schema" should {
    "treat sealed trait hierachies of case objects as an enum" in {
      implicit val schema = AvroSchema.toSchema[EnumCC]
      val expected =
        """
          |{"type":"record","name":"EnumCC","namespace":"unit.schema.SealedTraitEnum",
          |"fields":[{"name":"cupcat","type":{"type":"enum","name":"SimpleEnum","symbols":["Cupcat","Rendal"]}}]}""".stripMargin
          .replace("\n", "")

      schemaAsString[EnumCC] shouldBe expected
    }
    "allow name to be overriden with an annotation" in {
      implicit val schema = AvroSchema.toSchema[AnnotatedEnumCC]
      val expected =
        """{"type":"record","name":"AnnotatedEnumCC","namespace":"unit.schema.SealedTraitEnum",
          |"fields":[{"name":"cupcat","type":{"type":"enum","name":"CupcatEnum",
          |"symbols":["AnnotatedCupcat","AnnotatedRendal"]}}]}""".stripMargin.replace("\n", "")

      schemaAsString[AnnotatedEnumCC] shouldBe expected
    }
    "handle nested enums" in {
      implicit val schema = AvroSchema.toSchema[MultipleEnumRecord]
      val expected =
        """{"type":"record","name":"MultipleEnumRecord","namespace":"unit.schema.SealedTraitEnum",
          |"fields":[{"name":"s","type":"string"},{"name":"er","type":{"type":"enum","name":"Cup",
          |"symbols":["B","C"]}},{"name":"enumRecord","type":{"type":"record","name"
          |:"EnumRecord","fields":[{"name":"enumField","type":{"type":"enum","name":"Cup",
          |"namespace":"unit.schema.SealedTraitEnum.Cup2","symbols":["B","C"]}}]}}]}""".stripMargin.replace("\n", "")

      schemaAsString[MultipleEnumRecord] shouldBe expected

    }

  }

}

object SealedTraitEnum {

  sealed trait SimpleEnum
  case object Cupcat extends SimpleEnum
  case object Rendal extends SimpleEnum
  case class EnumCC(cupcat: SimpleEnum)

  @SchemaMetadata(Map(Name -> "CupcatEnum"))
  sealed trait AnnotatedEnum
  case object AnnotatedCupcat extends AnnotatedEnum
  case object AnnotatedRendal extends AnnotatedEnum
  case class AnnotatedEnumCC(cupcat: AnnotatedEnum)

  sealed trait Cup
  case object B extends Cup
  case object C extends Cup

  case class EnumRecord(@SchemaMetadata(Map(Namespace -> "unit.schema.SealedTraitEnum.Cup2")) enumField: Cup)
  case class MultipleEnumRecord(s: String, er: Cup, enumRecord: EnumRecord)
}
