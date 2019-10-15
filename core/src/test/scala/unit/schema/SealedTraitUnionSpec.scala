package unit.schema

import com.rauchenberg.avronaut.schema.AvroSchema
import unit.utils.UnitSpecBase
import unit.schema.SealedTraitUnion._

class SealedTraitUnionSpec extends UnitSpecBase {

  "schema" should {
    "treat mixed sealed trait hierachies as unions" in {
      implicit val schema = AvroSchema.toSchema[Union]
      val expected =
        """
          |{"type":"record","name":"Union","namespace":"unit.schema.SealedTraitUnion","doc":"",
          |"fields":[{"name":"cupcat","type":[{"type":"record","name":"Cupcat","doc":"","fields":[]},
          |{"type":"record","name":"Snoutley","doc":"","fields":[{"name":"name","type":"string"}]},
          |{"type":"record","name":"Rendal","doc":"","fields":[]}]}]}""".stripMargin.replace("\n", "")

      schemaAsString[Union] shouldBe expected
    }
    "handle mixed sealed trait unions with defaults" in {
      implicit val schema = AvroSchema.toSchema[UnionWithCaseClassDefault]
      val expected =
        """
          |{"type":"record","name":"UnionWithCaseClassDefault","namespace":"unit.schema.SealedTraitUnion","doc":"",
          |"fields":[{"name":"cupcat","type":[{"type":"record","name":"Snoutley","doc":"","fields":[{"name":"name","type":"string"}]},
          |{"type":"record","name":"Cupcat","doc":"","fields":[]},
          |{"type":"record","name":"Rendal","doc":"","fields":[]}],
          |"doc":"","default":{"name":"cupcat"}}]}""".stripMargin.replace("\n", "")

      schemaAsString[UnionWithCaseClassDefault] shouldBe expected
    }
    "handle mixed sealed trait unions with case object defaults" in {
      implicit val schema = AvroSchema.toSchema[UnionWithCaseObjectDefault]
      val expected =
        """
          |{"type":"record","name":"UnionWithCaseObjectDefault","namespace":"unit.schema.SealedTraitUnion","doc":"",
          |"fields":[{"name":"cupcat","type":[{"type":"record","name":"Rendal","doc":"","fields":[]},
          |{"type":"record","name":"Cupcat","doc":"","fields":[]},{"type":"record","name":"Snoutley","doc":"",
          |"fields":[{"name":"name","type":"string"}]}],"doc":"","default":{}}]}""".stripMargin.replace("\n", "")

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
