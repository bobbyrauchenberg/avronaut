package unit.decoder

import java.time.Instant

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.decoder.Parser
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.GenericData
import unit.TimeArbitraries._
import unit.decoder.utils.RunAssert._

class SealedTraitSpec extends UnitSpecBase {

  "decoder" should {

    import SealedTraitSpec._

    "handle sealed trait enums" in {
      forAll { enumRecord: EnumRecord =>
        runAssert(enumRecord.field.toString, enumRecord)
      }
    }

    "handle sealed trait enums with defaults" in {
      runAssert(B.toString, SealedTraitEnumWithDefault())
    }

    "handle out of order fields" in {
      forAll { w: WriterRecord =>
        val writerSchema = AvroSchema[WriterRecord].schema.value

        val genericRecord = new GenericData.Record(writerSchema)
        genericRecord.put(0, w.field1)
        genericRecord.put(1, w.field2.toEpochMilli)
        genericRecord.put(2, w.field3)
        genericRecord.put(3, w.field4.toString)

        val readerSchema = AvroSchema[ReaderRecord].schema.value

        val expected = ReaderRecord(w.field1, w.field2, w.field4)

        Parser.decode[ReaderRecord](readerSchema, genericRecord) should beRight(expected)
      }

    }

  }
}

private[this] object SealedTraitSpec {

  sealed trait A
  case object B extends A
  case object C extends A

  case class EnumRecord(field: A)

  case class SealedTraitEnumWithDefault(field: A = B)

  case class WriterRecord(field1: String, field2: Instant, field3: String, field4: A)

  case class ReaderRecord(field1: String, field2: Instant, field4: A)

}
