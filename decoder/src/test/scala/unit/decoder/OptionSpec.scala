package unit.decoder

import cats.syntax.option._
import unit.common.UnitSpecBase
import unit.decoder.utils.RunAssert.{beRight, runAssert}
import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.cupcatAvro.decoder.DecodeTo
import com.rauchenberg.cupcatAvro.schema.AvroSchema
import org.apache.avro.generic.GenericData

class OptionSpec extends UnitSpecBase {

  "decoder" should {
    "decode an union of null and T" in {
      forAll { i: Int =>
        val record = RecordWithOption(i.some)
        runAssert(i, record)
      }
    }
    "decode an option with a default" in {
      val schema = AvroSchema[RecordWithOptionDefault].schema.right.get
      val record = new GenericData.Record(schema)

      DecodeTo[RecordWithOptionDefault](record) should beRight(RecordWithOptionDefault(Option(123)))
    }
  }

  case class RecordWithOption(field: Option[Int])
  case class RecordWithOptionDefault(field: Option[Int] = Option(123))
}

