package unit

import common.{UnitSpecBase, schemaAsString}
import SealedTraitUnion._

class SealedTraitUnionSpec extends UnitSpecBase {

  "schema" should {
    "treat mixed sealed trait hierachies as unions" in {
      val expected =
        """
          |{"type":"record","name":"UnionCC","namespace":"unit.SealedTraitUnion","doc":"",
          |"fields":[{"name":"cupcat","type":[{"type":"record","name":"Cupcat","doc":"","fields":[]},
          |{"type":"record","name":"Snoutley","doc":"","fields":[{"name":"name","type":"string","doc":""}]},
          |{"type":"record","name":"Rendal","doc":"","fields":[]}],"doc":""}]}""".stripMargin.replace("\n","")

      schemaAsString[UnionCC] should beRight(expected)
    }

  }
}

private[this] object SealedTraitUnion {

  sealed trait SimpleEnum
  case object Cupcat extends SimpleEnum
  case class Snoutley(name: String) extends SimpleEnum
  case object Rendal extends SimpleEnum

  case class UnionCC(cupcat: SimpleEnum)

}

