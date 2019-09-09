package unit.decoder


import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.cupcatAvro.decoder.{DecodeTo}
import com.rauchenberg.cupcatAvro.schema.AvroSchema
import org.apache.avro.generic.GenericData
import shapeless.{:+:, CNil}
import unit.common.UnitSpecBase

class CoproductSpec extends UnitSpecBase {

  "decoder" should {

    "decode a union expressed with a coproduct" in new TestContext {
      import unit.decoder.utils.ShapelessArbitraries._

      forAll { (cp: Boolean :+: Int :+: String :+: CNil) =>
        runCoproductAssert(Union(cp))
      }
    }

    trait TestContext {
      def runCoproductAssert(expected: Union) = {

        val schema = AvroSchema[Union].schema

        val record = new GenericData.Record(schema.value)

        val field =
          expected.field.select[Boolean].orElse(expected.field.select[Int]).orElse(expected.field.select[String]).get

        record.put("field", field)

        DecodeTo[Union](record) should beRight(expected)
      }
    }

  }

  case class Union(field: Boolean :+: Int :+: String :+: CNil)

}
