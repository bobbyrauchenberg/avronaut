package unit.common

import com.rauchenberg.avronaut.Codec
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.encoder.Encoder
import org.scalacheck.Arbitrary
import unit.utils.UnitSpecBase
import Codec._

object RunRoundTripAssert extends UnitSpecBase {

  def runRoundTrip[A : Arbitrary](implicit encoder: Encoder[A], decoder: Decoder[A]) =
    forAll { record: A =>
      Encoder.encode(record, encoder).flatMap { genericRecord =>
        Decoder.decode[A](genericRecord, decoder)
      } should beRight(record)
    }

  def runCodecRoundTrip[A : Arbitrary](implicit codec: Codec[A]) =
    forAll { record: A =>
      println("encoded : " + record.encode)
      record.encode.flatMap { gr =>
        println(gr)
        println("decoded : " + gr.decode[A])
        gr.decode[A]
      } should beRight(record)
    }

}
