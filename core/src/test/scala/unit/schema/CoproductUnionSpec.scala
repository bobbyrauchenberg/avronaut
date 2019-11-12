package unit.schema

import com.rauchenberg.avronaut.common._
import com.rauchenberg.avronaut.schema.AvroSchema
import shapeless._
import unit.utils.UnitSpecBase
import unit.common._

class CoproductUnionSpec extends UnitSpecBase {

  "schema" should {

    "create a union from a Coproduct" in {
      implicit val schema = AvroSchema.toSchema[CoproductUnion]
      val expected =
        """
          |{"type":"record","name":"CoproductUnion","namespace":"unit.schema.CoproductUnionSpec",
          |"doc":"","fields":[{"name":"cupcat"
          |,"type":["string","boolean","int"]}]}""".stripMargin.replace("\n", "")

      schemaAsString[CoproductUnion] shouldBe expected
    }

    "respect a Coproduct default" in {
      implicit val schema = AvroSchema.toSchema[CoproductWithStringDefault]
      val expected =
        """
          |{"type":"record","name":"CoproductWithStringDefault","namespace":"unit.schema.CoproductUnionSpec",
          |"doc":"","fields":[{"name":"cupcat","type":["string","boolean","int"]
          |,"doc":"","default":"cupcat"}]}""".stripMargin.replace("\n", "")

      schemaAsString[CoproductWithStringDefault] shouldBe expected
    }

    "respect a Coproduct default regardless of place in the coproduct" in {
      implicit val schema = AvroSchema.toSchema[CoproductWithIntDefault]
      val expected =
        """
          |{"type":"record","name":"CoproductWithIntDefault","namespace":"unit.schema.CoproductUnionSpec",
          |"doc":"","fields":[{"name":"cupcat","type":["int","string","boolean"],"doc":""
          |,"default":123}]}""".stripMargin.replace("\n", "")

      schemaAsString[CoproductWithIntDefault] shouldBe expected
    }

    "flatten a nested structure of types that map to unions" in {
      implicit val schema = AvroSchema.toSchema[Nested]
      val expected =
        """
          |{"type":"record","name":"Nested","namespace":"unit.schema.CoproductUnionSpec","doc":"",
          |"fields":[{"name":"cupcat","type":["string","int","long","boolean"]}]}""".stripMargin.replace("\n", "")
      schemaAsString[Nested] shouldBe expected
    }

  }

  type CP                 = String :+: Boolean :+: Int :+: CNil
  type IllegalNestedCP    = String :+: (Boolean :+: Int :+: CNil) :+: Double :+: CNil
  type IllegalDuplicateCP = String :+: String :+: CNil

  type NestedCP = String :+: Either[Int, Long] :+: Boolean :+: CNil

  case class CoproductUnion(cupcat: CP)
  case class CoproductWithStringDefault(cupcat: CP = "cupcat".toCP[CP])
  case class CoproductWithIntDefault(cupcat: CP = 123.toCP[CP])
  case class IllegalNestedUnion(cupcat: IllegalNestedCP)
  case class IllegalDuplicateUnion(cupcat: IllegalDuplicateCP)

  case class Nested(cupcat: NestedCP)
}
