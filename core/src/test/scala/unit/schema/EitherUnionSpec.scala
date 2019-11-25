package unit.schema

import cats.syntax.either._
import com.rauchenberg.avronaut.schema.AvroSchema
import unit.utils.UnitSpecBase
import unit.common._

class EitherUnionSpec extends UnitSpecBase {

  "schema" should {

    "create a union from an Either" in {
      implicit val schema = AvroSchema.toSchema[EitherUnion]
      val expected =
        """
          |{"type":"record","name":"EitherUnion","namespace":"unit.schema.EitherUnionSpec",
          |"fields":[{"name":"cupcat","type":["boolean","string"]}]}""".stripMargin.replace("\n", "")

      schemaAsString[EitherUnion] shouldBe expected
    }

    "create a union from an Either with a Left default" in {
      implicit val schema = AvroSchema.toSchema[EitherUnionWithLeftDefault]
      val expected =
        """
          |{"type":"record","name":"EitherUnionWithLeftDefault","namespace":"unit.schema.EitherUnionSpec",
          |"fields":[{"name":"cupcat","type":["boolean","string"],"default":true}]}""".stripMargin
          .replace("\n", "")

      schemaAsString[EitherUnionWithLeftDefault] shouldBe expected
    }

    "create a union from an Either with a Right default" in {
      implicit val schema = AvroSchema.toSchema[EitherUnionWithRightDefault]
      val expected =
        """
          |{"type":"record","name":"EitherUnionWithRightDefault","namespace":"unit.schema.EitherUnionSpec",
          |"fields":[{"name":"cupcat","type":["string","boolean"],"default":"cupcat"}]}""".stripMargin
          .replace("\n", "")

      schemaAsString[EitherUnionWithRightDefault] shouldBe expected
    }

    "flatten a nested structure of types that map to unions" in {
      implicit val schema = AvroSchema.toSchema[NestedUnion]
      val expected =
        """
          |{"type":"record","name":"NestedUnion","namespace":"unit.schema.EitherUnionSpec",
          |"fields":[{"name":"cupcat","type":["boolean","int","string"]}]}""".stripMargin.replace("\n", "")

      schemaAsString[NestedUnion] shouldBe expected
    }

  }

  case class EitherUnion(cupcat: Either[Boolean, String])
  case class EitherUnionWithLeftDefault(cupcat: Either[Boolean, String] = true.asLeft)
  case class EitherUnionWithRightDefault(cupcat: Either[Boolean, String] = "cupcat".asRight)
  case class NestedUnion(cupcat: Either[Boolean, Either[Int, String]])
  case class IllegalDuplicateUnion(cupcat: Either[Boolean, Either[String, String]])

  case class Cupcat(field1: Boolean, field2: Float)
  case class Rendal(field1: Boolean, field2: String)
  case class UnionOfRecords(field: Either[Cupcat, Rendal])

}
