package unit.schema

import com.rauchenberg.cupcatAvro.common._
import common.{UnitSpecBase, schemaAsString}
import shapeless._

class CoproductUnionSpec extends UnitSpecBase {

  "schema" should {

    "create a union from a Coproduct" in {
      val expected =
        """
          |{"type":"record","name":"CoproductUnion","namespace":"unit.schema.CoproductUnionSpec",
          |"doc":"","fields":[{"name":"cupcat"
          |,"type":["int","boolean","string"]}]}""".stripMargin.replace("\n", "")

      schemaAsString[CoproductUnion] should beRight(expected)
    }

    "respect a Coproduct default" in {
      val expected =
        """
          |{"type":"record","name":"CoproductWithStringDefault","namespace":"unit.schema.CoproductUnionSpec",
          |"doc":"","fields":[{"name":"cupcat","type":["string","int","boolean"]
          |,"doc":"","default":"cupcat"}]}""".stripMargin.replace("\n", "")

      schemaAsString[CoproductWithStringDefault] should beRight(expected)
    }

    "respect a Coproduct default regardless of place in the coproduct" in {
      val expected =
        """
          |{"type":"record","name":"CoproductWithIntDefault","namespace":"unit.schema.CoproductUnionSpec",
          |"doc":"","fields":[{"name":"cupcat","type":["int","boolean","string"],"doc":""
          |,"default":123}]}""".stripMargin.replace("\n", "")

      schemaAsString[CoproductWithIntDefault] should beRight(expected)
    }

    "flatten a nested structure of types that map to unions" in {
      val expected =
        """
          |{"type":"record","name":"Nested","namespace":"unit.schema.CoproductUnionSpec","doc":"",
          |"fields":[{"name":"cupcat","type":["boolean","int","long","string"]}]}""".stripMargin.replace("\n", "")
      schemaAsString[Nested] should beRight(expected)
    }

    "error if a union contains a duplicate" in {
      schemaAsString[IllegalDuplicateCP] should beLeft(Error("Duplicate in union:string"))
    }

  }

  type CP = String :+: Boolean :+: Int :+: CNil
  type IllegalNestedCP = String :+: (Boolean :+: Int :+: CNil) :+: Double :+: CNil
  type IllegalDuplicateCP = String :+: String :+: CNil

  type NestedCP = String :+: Either[Int, Long] :+: Boolean :+: CNil

  case class CoproductUnion(cupcat: CP)
  case class CoproductWithStringDefault(cupcat: CP = "cupcat".toCP[CP])
  case class CoproductWithIntDefault(cupcat: CP = 123.toCP[CP])
  case class IllegalNestedUnion(cupcat: IllegalNestedCP)
  case class IllegalDuplicateUnion(cupcat: IllegalDuplicateCP)

  case class Nested(cupcat: NestedCP)
}

