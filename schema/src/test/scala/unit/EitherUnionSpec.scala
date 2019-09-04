package unit

import common.UnitSpecBase
import cats.syntax.either._
import EitherUnion._
import com.rauchenberg.cupcatAvro.schema.SchemaError
import common._

class EitherUnionSpec extends UnitSpecBase {

  "schema" should {

    "create a union from an Either" in {
      val expected =
        """
          |{"type":"record","name":"EitherUnion","namespace":"unit.EitherUnion",
          |"doc":"","fields":[{"name":"cupcat","type":["boolean","string"]}]}""".stripMargin.replace("\n","")

      schemaAsString[EitherUnion] should beRight(expected)
    }

    "create a union from an Either with a Left default" in {
      val expected =
        """
          |{"type":"record","name":"EitherUnionWithLeftDefault","namespace":"unit.EitherUnion",
          |"doc":"","fields":[{"name":"cupcat","type":["boolean","string"],"doc":"","default":true}]}""".stripMargin.replace("\n","")

      schemaAsString[EitherUnionWithLeftDefault] should beRight(expected)
    }

    "create a union from an Either with a Right default" in {
      val expected =
        """
          |{"type":"record","name":"EitherUnionWithRightDefault","namespace":"unit.EitherUnion",
          |"doc":"","fields":[{"name":"cupcat","type":["string","boolean"],"doc":"","default":"cupcat"}]}""".stripMargin.replace("\n","")

      schemaAsString[EitherUnionWithRightDefault] should beRight(expected)
    }

    "error if a union contains a union" in {
      schemaAsString[IllegalNestedUnion] should beLeft(SchemaError("""Nested union: ["boolean",["int","string"]]"""))
    }

    "error if a union contains a duplicate" in {
      schemaAsString[IllegalDuplicateUnion] should beLeft(SchemaError("Duplicate in union:string"))
    }
  }

}

private [this] object EitherUnion {

  case class EitherUnion(cupcat: Either[Boolean, String])
  case class EitherUnionWithLeftDefault(cupcat: Either[Boolean, String] = true.asLeft)
  case class EitherUnionWithRightDefault(cupcat: Either[Boolean, String] = "cupcat".asRight)
  case class IllegalNestedUnion(cupcat: Either[Boolean, Either[Int, String]])
  case class IllegalDuplicateUnion(cupcat: Either[Boolean, Either[String, String]])

}
