package unit.schema

import com.rauchenberg.avronaut.schema.AvroSchema
import unit.utils.UnitSpecBase
import unit.schema.SealedTraitUnion._
import unit.common._

class SealedTraitUnionSpec extends UnitSpecBase {

  "schema" should {
    "treat mixed sealed trait hierachies as unions" in {
      implicit val schema = AvroSchema.toSchema[Union]
      val expected =
        """
          |{"type":"record","name":"Union","namespace":"unit.schema.SealedTraitUnion",
          |"fields":[{"name":"cupcat","type":[{"type":"record","name":"Cupcat","fields":[]},
          |{"type":"record","name":"Snoutley","fields":[{"name":"name","type":"string"}]},
          |{"type":"record","name":"Rendal","fields":[]}]}]}""".stripMargin.replace("\n", "")

      schemaAsString[Union] shouldBe expected
    }
    "handle mixed sealed trait unions with defaults" in {
      implicit val schema = AvroSchema.toSchema[UnionWithCaseClassDefault]
      val expected =
        """
          |{"type":"record","name":"UnionWithCaseClassDefault","namespace":"unit.schema.SealedTraitUnion","fields"
          |:[{"name":"cupcat","type":[{"type":"record","name":"Snoutley","fields":
          |[{"name":"name","type":"string"}]},{"type":"record","name":"Cupcat","fields":[]},
          |{"type":"record","name":"Rendal","fields":[]}],
          |"default":{"name":"cupcat"}}]}""".stripMargin.replace("\n", "")

      schemaAsString[UnionWithCaseClassDefault] shouldBe expected
    }
    "handle mixed sealed trait unions with case object defaults" in {
      implicit val schema = AvroSchema.toSchema[UnionWithCaseObjectDefault]
      val expected =
        """
          |{"type":"record","name":"UnionWithCaseObjectDefault","namespace":"unit.schema.SealedTraitUnion",
          |"fields":[{"name":"cupcat","type":[{"type":"record","name":"Rendal","fields":[]},
          |{"type":"record","name":"Cupcat","fields":[]},{"type":"record","name":"Snoutley",
          |"fields":[{"name":"name","type":"string"}]}],"default":{}}]}""".stripMargin.replace("\n", "")

      schemaAsString[UnionWithCaseObjectDefault] shouldBe expected
    }
  }
}

object SealedTraitUnion {

  sealed trait SimpleEnum
  case object Cupcat                extends SimpleEnum
  case class Snoutley(name: String) extends SimpleEnum
  case object Rendal                extends SimpleEnum

  case class Union(cupcat: SimpleEnum)
  case class UnionWithCaseClassDefault(cupcat: SimpleEnum = Snoutley("cupcat"))
  case class UnionWithCaseObjectDefault(cupcat: SimpleEnum = Rendal)

}
