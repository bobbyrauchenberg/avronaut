package unit.decoder

import com.danielasfregola.randomdatagenerator.RandomDataGenerator._
import com.rauchenberg.avronaut.Codec
import com.rauchenberg.avronaut.Codec._
import com.rauchenberg.avronaut.schema.AvroSchema
import com.sksamuel.avro4s.{DefaultFieldMapper, FromRecord, SchemaFor, ToRecord}
import org.apache.avro.generic.{GenericData, GenericRecordBuilder}
import unit.decoder.SealedTraitUnionSpec.{A, B, InSeason, InSeasonEpisode, OutOfSeason, Type, Unknown, X, XCC, XWrapper}
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
//        val cond = x.t match {
//          case OutOfSeason(_, episode) if (episode.isRight) => true
//          case _                                            => false
//        }
//        whenever(cond) {
        val rec = XCC(InSeason("", Right(InSeasonEpisode(0, false))))
//        val fromRecord = FromRecord[XCC]
//
//        println(ToRecord[XCC].to(rec))
//
//        fromRecord.from(ToRecord[XCC].to(rec)) shouldBe rec

        rec.encode.flatMap(_.decodeAccumulating[XCC]) should beRight(rec)
//        }
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

  case object Unknown                                                                                extends Type
  case object Movie                                                                                  extends Type
  case object StandaloneProgramme                                                                    extends Type
  final case class InSeason(seasonId: String, episode: Either[SpecialEpisode.type, InSeasonEpisode]) extends Type

  final case class OutOfSeason(seriesId: String, episode: Either[SpecialEpisode.type, OutOfSeasonEpisode]) extends Type
  final case class OutOfSeasonEpisode(episodeNumber: Int)

  final case class InSeasonEpisode(episodeNumber: Int, lastInSeason: Boolean)
  case object SpecialEpisode

}
