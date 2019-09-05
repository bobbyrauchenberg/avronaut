package unit.schema

import cats.syntax.option._
import common._
import common.UnitSpecBase
import com.rauchenberg.cupcatAvro.schema.SchemaError

class OptionUnionSpec extends UnitSpecBase {

  "schema" should {
    "create a union from an Option" in {
      val expected =
        """
          |{"type":"record","name":"Union","namespace":"unit.schema.OptionUnionSpec",
          |"doc":"","fields":[{"name":"cupcat","type":["null","string"]}]}""".stripMargin.replace("\n","")

      schemaAsString[Union] should beRight(expected)
    }

    "create a union from an Option with a default" in {
      val expected =
        """
          |{"type":"record","name":"UnionWithDefault","namespace":"unit.schema.OptionUnionSpec","doc":"",
          |"fields":[{"name":"cupcat","type":["string","null"],"doc":"","default":"cupcat"}]}""".stripMargin.replace("\n","")

      schemaAsString[UnionWithDefault] should beRight(expected)
    }

    "create a union from an Option with None as default" in {
      val expected =
        """
          |{"type":"record","name":"UnionWithDefaultNone","namespace":"unit.schema.OptionUnionSpec","doc":"",
          |"fields":[{"name":"cupcat","type":["null","string"],"doc":"","default":null}]}""".stripMargin.replace("\n","")

      schemaAsString[UnionWithDefaultNone] should beRight(expected)
    }

    "create a union from an Option with a default case class" in {
      val expected =
        """
          |{"type":"record","name":"UnionWithCaseClassDefault","namespace":"unit.schema.OptionUnionSpec","doc":"",
          |"fields":[{"name":"cupcat","type":{"type":"record","name":"Default","doc":"",
          |"fields":[{"name":"cupcat","type":"string"}]},"doc":"",
          |"default":{"cupcat":"cupcat"}}]}""".stripMargin.replace("\n","")

      schemaAsString[UnionWithCaseClassDefault] should beRight(expected)
    }

    "error if a union contains a union" in {
      schemaAsString[IllegalNestedUnion] should beLeft(SchemaError("""Nested union: ["null",["null","string"]]"""))
    }
  }

  case class Union(cupcat : Option[String])
  case class UnionWithDefault(cupcat: Option[String] = "cupcat".some)
  case class UnionWithDefaultNone(cupcat: Option[String] = None)

  case class Default(cupcat: String)
  case class UnionWithCaseClassDefault(cupcat: Default = Default("cupcat"))

  case class IllegalNestedUnion(cupcat: Option[Option[String]])
}
