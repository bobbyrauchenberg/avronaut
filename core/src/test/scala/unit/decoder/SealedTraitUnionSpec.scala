package unit.decoder

import com.danielasfregola.randomdatagenerator.RandomDataGenerator._
import com.rauchenberg.avronaut.Codec
import com.rauchenberg.avronaut.Codec._
import com.rauchenberg.avronaut.schema.AvroSchema
import com.sksamuel.avro4s.{DefaultFieldMapper, FromRecord, SchemaFor, ToRecord}
import org.apache.avro.generic.{GenericData, GenericRecordBuilder}
import unit.decoder.SealedTraitUnionSpec.{A, B, F, I, G, Type, C, X, XCC, XWrapper}
import unit.utils.UnitSpecBase

class SealedTraitUnionSpec extends UnitSpecBase {

  "decoder" should {
    "decode a sealed trait union" in {
      implicit val codec = Codec[XWrapper]
      val schema         = Codec.schema[XWrapper].value

      forAll { record: XWrapper =>
        val genericRecord = new GenericRecordBuilder(schema)

        record.field match {
          case A => genericRecord.set("field", record.field.toString)
          case B(field) =>
            val bSchema = AvroSchema.toSchema[B]
            val bGR     = new GenericData.Record(bSchema.data.value.schema)
            bGR.put(0, field)
            genericRecord.set("field", bGR)
        }

        genericRecord.build.decode[XWrapper] should beRight(record)
      }
    }

    "be blah" in {

      forAll { x: XCC =>
        x.encode.flatMap(_.decodeAccumulating[XCC]) should beRight(x)
      }
    }

  }

}

object SealedTraitUnionSpec {

  case class XCC(t: Type)
  implicit val xCodec: Codec[XCC] = Codec[XCC]

  sealed trait X
  case object A               extends X
  case class B(field: String) extends X

  case class XWrapper(field: X)

  sealed trait Type extends Product with Serializable
  case object C                                                                                extends Type
  case object D                                                                                  extends Type
  case object E                                                                    extends Type
  final case class F(seasonId: String, episode: Either[SpecialEpisode.type, I]) extends Type
  final case class G(seriesId: String, episode: Either[SpecialEpisode.type, H]) extends Type
  final case class H(episodeNumber: Int)
  final case class I(episodeNumber: Int, lastInSeason: Boolean)
  case object SpecialEpisode

}
