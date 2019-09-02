package unit

import common.{UnitSpecBase, schemaAsString}
import shapeless._
import CoproductUnion._

class CoproductUnionSpec extends UnitSpecBase {

  "schema" should {

    "create a union from a Coproduct" in {
      val expected =
        """
          |{"type":"record","name":"CoproductUnion","namespace":"unit.CoproductUnion",
          |"doc":"","fields":[{"name":"cupcat","type":["int","boolean","string"],"doc":""}]}""".stripMargin.replace("\n", "")

      schemaAsString[CoproductUnion] should beRight(expected)
    }
  }
}

private [this] object CoproductUnion {

  case class CoproductUnion(cupcat: String :+: Boolean :+: Int :+: CNil)

}