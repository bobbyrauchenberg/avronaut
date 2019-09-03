package unit

import common._
import common.UnitSpecBase
import SealedTraitEnum._

class SealedTraitEnumSpec extends UnitSpecBase {

  "schema" should {
    "treat sealed trait hierachies of case objects as an enum" in {
      val expected =
        """
          |{"type":"record","name":"EnumCC","namespace":"unit.SealedTraitEnum","doc":"",
          |"fields":[{"name":"cupcat","type":{"type":"enum","name":"SealedTraitEnum","namespace":"unit","doc":"",
          |"symbols":["Cupcat","Rendal"]},"doc":""}]}""".stripMargin.replace("\n","")

      schemaAsString[EnumCC] should beRight(expected)
    }
  }

}

private [this] object SealedTraitEnum {

  sealed trait SimpleEnum
  case object Cupcat extends SimpleEnum
  case object Rendal extends SimpleEnum

  case class EnumCC(cupcat: SimpleEnum)
}
