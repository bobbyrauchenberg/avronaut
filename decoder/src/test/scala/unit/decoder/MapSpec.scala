package unit.decoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import unit.common.UnitSpecBase
import unit.decoder.utils.RunAssert._

class MapSpec extends UnitSpecBase {

  "decoder" should {
    "decode a record with a map" in {
      forAll { record: RecordWithMap =>
        runAssert(record.field, record)
      }
    }
    "decode a record with a map with a default" in {
      forAll { record: RecordWithDefaultMap =>
        runAssert(record.field, record)
      }
    }
  }

  case class RecordWithMap(field: Map[String, String])
  case class RecordWithDefaultMap(field: Map[String, String] = Map("cup" -> "cat"))
}

