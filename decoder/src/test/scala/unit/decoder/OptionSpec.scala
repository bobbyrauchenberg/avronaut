package unit.decoder

import cats.syntax.option._
import unit.common.UnitSpecBase
import unit.decoder.utils.RunAssert.runAssert
import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.cupcatAvro.decoder.{DecodeTo, Decoder}
import com.rauchenberg.cupcatAvro.schema.AvroSchema
import org.apache.avro.generic.GenericData

class OptionSpec extends UnitSpecBase {

  "decoder" should {
    "decode an union of null and T" in {
      forAll { i: Int =>
        val record = Union(i.some)
        runAssert(i, record)
      }
    }
    "decode an option with a default" in {
      val schema = AvroSchema[UnionWithDefault].schema.right.get
      val record = new GenericData.Record(schema)

      DecodeTo[UnionWithDefault](record) should beRight(UnionWithDefault())
    }
    "decode an option with a caseclass default" in new TestContext {
      runAssert[UnionWithCaseClassDefault](UnionWithCaseClassDefault())
    }
    "decode an option with none as default" in new TestContext {
      runAssert[UnionWithNoneAsDefault](UnionWithNoneAsDefault(None))
    }

    trait TestContext {
      def runAssert[T : AvroSchema : Decoder](expected: T)  = {
        val eitherSchema = AvroSchema[T].schema
        eitherSchema.isRight shouldBe true

        val schema = eitherSchema.right.get
        val record = new GenericData.Record(schema)

        DecodeTo[T](record) should beRight(expected)
      }

    }

  }

  case class Union(field: Option[Int])

  case class UnionWithDefault(field: Option[Int] = Option(123))

  case class DefaultValue(cup: String, cat: String)

  case class UnionWithNoneAsDefault(field: Option[DefaultValue] = None)

  case class UnionWithCaseClassDefault(field: Option[DefaultValue] = Option(DefaultValue("cup", "cat")))

}

