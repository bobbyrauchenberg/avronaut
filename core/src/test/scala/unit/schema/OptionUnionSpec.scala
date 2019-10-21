package unit.schema

import cats.syntax.option._
import com.rauchenberg.avronaut.schema.AvroSchema
import unit.utils.UnitSpecBase
import unit.common._

class OptionUnionSpec extends UnitSpecBase {

  "schema" should {
    "create a union from an Option" in {
      implicit val schema = AvroSchema.toSchema[Union]
      val expected =
        """
          |{"type":"record","name":"Union","namespace":"unit.schema.OptionUnionSpec",
          |"doc":"","fields":[{"name":"cupcat","type":["null","string"]}]}""".stripMargin.replace("\n", "")

      schemaAsString[Union] shouldBe expected
    }

    "create a union from an Option with a string default" in {
      implicit val schema = AvroSchema.toSchema[UnionWithStringDefault]
      val expected =
        """
          |{"type":"record","name":"UnionWithStringDefault","namespace":"unit.schema.OptionUnionSpec","doc":"",
          |"fields":[{"name":"cupcat","type":["string","null"],"doc":"","default":"cupcat"}]}""".stripMargin
          .replace("\n", "")

      schemaAsString[UnionWithStringDefault] shouldBe expected
    }

    "create a union from an Option with an int default" in {
      implicit val schema = AvroSchema.toSchema[UnionWithIntDefault]
      val expected =
        """
          |{"type":"record","name":"UnionWithIntDefault","namespace":"unit.schema.OptionUnionSpec","doc":"",
          |"fields":[{"name":"cupcat","type":["int","null"],"doc":"","default":123}]}""".stripMargin.replace("\n", "")

      schemaAsString[UnionWithIntDefault] shouldBe expected
    }

    "create a union from an Option with a Byte Array default" in {
      implicit val schema = AvroSchema.toSchema[UnionWithByteArrayDefault]
      val expected =
        """
          |{"type":"record","name":"UnionWithByteArrayDefault","namespace":"unit.schema.OptionUnionSpec","doc":"",
          |"fields":[{"name":"cupcat","type":["bytes","null"],"doc":"","default":"cupcat"}]}""".stripMargin
          .replace("\n", "")

      schemaAsString[UnionWithByteArrayDefault] shouldBe expected
    }

    "create a union from an Option with None as default" in {
      implicit val schema = AvroSchema.toSchema[UnionWithDefaultNone]
      val expected =
        """
          |{"type":"record","name":"UnionWithDefaultNone","namespace":"unit.schema.OptionUnionSpec","doc":"",
          |"fields":[{"name":"cupcat","type":["null","string"],"doc":"","default":null}]}""".stripMargin.replace("\n",
                                                                                                                 "")

      schemaAsString[UnionWithDefaultNone] shouldBe expected
    }

    "create a union from an Option with a default case class" in {
      implicit val schema = AvroSchema.toSchema[UnionWithCaseClassDefault]
      val expected =
        """
          |{"type":"record","name":"UnionWithCaseClassDefault","namespace":"unit.schema.OptionUnionSpec","doc":"",
          |"fields":[{"name":"cupcat","type":{"type":"record","name":"Default","doc":"",
          |"fields":[{"name":"cupcat","type":"string"}]},"doc":"",
          |"default":{"cupcat":"cupcat"}}]}""".stripMargin.replace("\n", "")

      schemaAsString[UnionWithCaseClassDefault] shouldBe expected
    }

    "create a union from an Option with a default optional case class" in {
      implicit val schema = AvroSchema.toSchema[UnionWithOptionalCaseClassDefault]
      val expected =
        """{"type":"record","name":"UnionWithOptionalCaseClassDefault","namespace":"unit.schema.OptionUnionSpec",
          |"doc":"","fields":[{"name":"cupcat","type":[{"type":"record","name":"Default","doc":"",
          |"fields":[{"name":"cupcat","type":"string"}]},"null"],
          |"doc":"","default":{"cupcat":"cupcat"}}]}""".stripMargin.replace("\n", "")

      schemaAsString[UnionWithOptionalCaseClassDefault] shouldBe expected
    }

    "flatten a nested structure of types that are encoded as unions" in {
      implicit val schema = AvroSchema.toSchema[NestedUnion]
      val expected =
        """
          |{"type":"record","name":"NestedUnion","namespace":"unit.schema.OptionUnionSpec","doc":"",
          |"fields":[{"name":"cupcat","type":["null","boolean","string"]}]}""".stripMargin.replace("\n", "")

      schemaAsString[NestedUnion] shouldBe expected
    }

  }

  case class Union(cupcat: Option[String])
  case class UnionWithStringDefault(cupcat: Option[String] = "cupcat".some)
  case class UnionWithIntDefault(cupcat: Option[Int] = 123.some)
  case class UnionWithByteArrayDefault(cupcat: Option[Array[Byte]] = "cupcat".getBytes.some)

  case class UnionWithDefaultNone(cupcat: Option[String] = None)

  case class Default(cupcat: String)
  case class UnionWithCaseClassDefault(cupcat: Default = Default("cupcat"))
  case class UnionWithOptionalCaseClassDefault(cupcat: Option[Default] = Default("cupcat").some)

  case class NestedUnion(cupcat: Option[Either[Boolean, String]])
}
