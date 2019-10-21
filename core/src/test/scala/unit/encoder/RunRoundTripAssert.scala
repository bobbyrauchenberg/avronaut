package unit.encoder

import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.scalacheck.Arbitrary
import unit.utils.UnitSpecBase

object RunRoundTripAssert extends UnitSpecBase {

  def runRoundTrip[A : Arbitrary](implicit encoder: Encoder[A], schema: AvroSchema[A], decoder: Decoder[A]) =
    forAll { record: A =>
      Encoder.encode(record, encoder, schema.data).flatMap { genericRecord =>
        Decoder.decode[A](genericRecord, decoder)
      } should beRight(record)
    }

}
