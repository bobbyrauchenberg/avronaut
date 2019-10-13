package unit.encoder

import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.scalacheck.Arbitrary
import unit.utils.UnitSpecBase

object RunRoundTripAssert extends UnitSpecBase {

  def runRoundTrip[A : AvroSchema : Arbitrary : Encoder : Decoder] = {
    val schema = AvroSchema.toSchema[A].value
    forAll { record: A =>
      Encoder.encode(record, schema).flatMap { genericRecord =>
        Decoder.decode[A](genericRecord, schema)
      } should beRight(record)
    }
  }

}
