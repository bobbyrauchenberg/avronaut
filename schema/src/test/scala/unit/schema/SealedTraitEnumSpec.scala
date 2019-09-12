package unit.schema

import common._
import common.UnitSpecBase
import SealedTraitEnum._
import com.rauchenberg.cupcatAvro.common.annotations.SchemaAnnotations.{Name, SchemaMetadata}

class SealedTraitEnumSpec extends UnitSpecBase {

  "schema" should {
    "treat sealed trait hierachies of case objects as an enum" in {
      val expected =
        """
          |{"type":"record","name":"EnumCC","namespace":"unit.schema.SealedTraitEnum","doc":"",
          |"fields":[{"name":"cupcat","type":{"type":"enum","name":"SimpleEnum",
          |"symbols":["Cupcat","Rendal"]}}]}""".stripMargin.replace("\n", "")

      schemaAsString[EnumCC] should beRight(expected)
    }
    "allow name to be overriden with an annotation" in {
      val expected =
        """{"type":"record","name":"AnnotatedEnumCC","namespace":"unit.schema.SealedTraitEnum","doc":"",
          |"fields":[{"name":"cupcat","type":{"type":"enum","name":"CupcatEnum",
          |"symbols":["AnnotatedCupcat","AnnotatedRendal"]}}]}""".stripMargin.replace("\n", "")

      schemaAsString[AnnotatedEnumCC] should beRight(expected)
    }
  }

}

private[this] object SealedTraitEnum {

  sealed trait SimpleEnum
  case object Cupcat extends SimpleEnum
  case object Rendal extends SimpleEnum
  case class EnumCC(cupcat: SimpleEnum)

  @SchemaMetadata(Map(Name -> "CupcatEnum"))
  sealed trait AnnotatedEnum
  case object AnnotatedCupcat extends AnnotatedEnum
  case object AnnotatedRendal extends AnnotatedEnum
  case class AnnotatedEnumCC(cupcat: AnnotatedEnum)
}
