package unit.schema

import com.rauchenberg.cupcatAvro.common._
import cats.syntax.either._
import common.{UnitSpecBase, _}

class EitherUnionSpec extends UnitSpecBase {

  "schema" should {

    "create a union from an Either" in {
      val expected =
        """
          |{"type":"record","name":"EitherUnion","namespace":"unit.schema.EitherUnionSpec",
          |"doc":"","fields":[{"name":"cupcat","type":["boolean","string"]}]}""".stripMargin.replace("\n","")

      schemaAsString[EitherUnion] should beRight(expected)
    }

    "create a union from an Either with a Left default" in {
      val expected =
        """
          |{"type":"record","name":"EitherUnionWithLeftDefault","namespace":"unit.schema.EitherUnionSpec",
          |"doc":"","fields":[{"name":"cupcat","type":["boolean","string"],"doc":"","default":true}]}""".stripMargin.replace("\n","")

      schemaAsString[EitherUnionWithLeftDefault] should beRight(expected)
    }

    "create a union from an Either with a Right default" in {
      val expected =
        """
          |{"type":"record","name":"EitherUnionWithRightDefault","namespace":"unit.schema.EitherUnionSpec",
          |"doc":"","fields":[{"name":"cupcat","type":["string","boolean"],"doc":"","default":"cupcat"}]}""".stripMargin.replace("\n","")

      schemaAsString[EitherUnionWithRightDefault] should beRight(expected)
    }

    "flatten a nested structure of types that map to unions" in {
      val expected =
        """
          |{"type":"record","name":"NestedUnion","namespace":"unit.schema.EitherUnionSpec",
          |"doc":"","fields":[{"name":"cupcat","type":["boolean","int","string"]}]}""".stripMargin.replace("\n", "")

      schemaAsString[NestedUnion] should beRight(expected)
    }

    "error if a union contains a duplicate" in {
      schemaAsString[IllegalDuplicateUnion] should beLeft(Error("Duplicate in union:string"))
    }
  }

  case class EitherUnion(cupcat: Either[Boolean, String])
  case class EitherUnionWithLeftDefault(cupcat: Either[Boolean, String] = true.asLeft)
  case class EitherUnionWithRightDefault(cupcat: Either[Boolean, String] = "cupcat".asRight)
  case class NestedUnion(cupcat: Either[Boolean, Either[Int, String]])
  case class IllegalDuplicateUnion(cupcat: Either[Boolean, Either[String, String]])

}
