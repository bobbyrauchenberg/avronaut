package unit.decoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import unit.common.UnitSpecBase
import unit.decoder.utils.RunAssert._

class SimpleRecordSpec extends UnitSpecBase {

  "decoder" should {
    "decode a record with a string field" in {
      forAll { record: StringRecord =>
        runAssert(record.field, record)
      }
    }
    "decode a record with a boolean field" in {
      forAll { record: BooleanRecord =>
        runAssert(record.field, record)
      }
    }
    "decode a record with an int field" in {
      forAll { record: IntRecord =>
        runAssert(record.field, record)
      }
    }
    "decode a record with a long field" in {
      forAll { record: LongRecord =>
        runAssert(record.field, record)
      }
    }
    "decode a record with a float field" in {
      forAll { record: FloatRecord =>
        runAssert(record.field, record)
      }
    }
    "decode a record with a double field" in {
      forAll { record: DoubleRecord =>
        runAssert(record.field, record)
      }
    }
    "decode a record with a byte array field" in {
      forAll { record: BytesRecord =>
        runAssert(record.field, record)
      }
    }
  }

  case class BooleanRecord(field: Boolean)
  case class IntRecord(field: Int)
  case class LongRecord(field: Long)
  case class FloatRecord(field: Float)
  case class DoubleRecord(field: Double)
  case class StringRecord(field: String)
  case class BytesRecord(field: Array[Byte])
  case class NestedRecord(field: IntRecord)
}

