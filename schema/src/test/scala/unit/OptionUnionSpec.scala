package unit

import cats.syntax.option._
import common._
import common.UnitSpecBase
import OptionUnion._
import com.rauchenberg.cupcatAvro.schema.SchemaError

class OptionUnionSpec extends UnitSpecBase {

  "schema" should {
    "create a union from an Option" in {
      val expected =
        """
          |{"type":"record","name":"OptionUnion","namespace":"unit.OptionUnion",
          |"doc":"","fields":[{"name":"cupcat","type":["null","string"],"doc":""}]}""".stripMargin.replace("\n","")

      schemaAsString[OptionUnion] should beRight(expected)
    }

    "create a union from an Option with a default" in {
      val expected =
        """
          |{"type":"record","name":"OptionUnionWithDefault","namespace":"unit.OptionUnion",
          |"doc":"","fields":[{"name":"cupcat","type":["string","null"],"doc":"","default":"cupcat"}]}""".stripMargin.replace("\n","")

      schemaAsString[OptionUnionWithDefault] should beRight(expected)
    }

    "create a union from an Option with None as default" in {
      val expected =
        """
          |{"type":"record","name":"OptionUnionWithDefaultNone","namespace":"unit.OptionUnion","doc":"",
          |"fields":[{"name":"cupcat","type":["null","string"],"doc":"","default":null}]}""".stripMargin.replace("\n","")

      schemaAsString[OptionUnionWithDefaultNone] should beRight(expected)
    }

    "error if a union contains a union" in {
      schemaAsString[IllegalNestedUnion] should beLeft(SchemaError("""Nested union: ["null",["null","string"]]"""))
    }
  }

}

private [this] object OptionUnion {
  case class OptionUnion(cupcat : Option[String])
  case class OptionUnionWithDefault(cupcat: Option[String] = "cupcat".some)
  case class OptionUnionWithDefaultNone(cupcat: Option[String] = None)
  case class IllegalNestedUnion(cupcat: Option[Option[String]])
}
