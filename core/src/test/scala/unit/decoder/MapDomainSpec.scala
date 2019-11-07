//package unit.decoder
//
//import java.time.Duration
//
//import com.rauchenberg.avronaut.common.Results
//import com.rauchenberg.avronaut.decoder.Decoder
//import com.rauchenberg.avronaut.encoder.{Encoder, EncoderBuilder}
//import com.rauchenberg.avronaut.schema.{AvroSchemaADT, SchemaBuilder, SchemaData, SchemaLong}
//import unit.domain.{CanonicalEntity, OfferEntity}
//import unit.utils.UnitSpecBase
//
//class MapDomainSpec extends UnitSpecBase {
//
//  "decoder" should {
//    "decode the map domain" in {
//
//      import MapDomainSpec._
//      implicit val e  = Encoder[OfferEntity]
//      implicit val ce = Encoder[CanonicalEntity]
//      println(e.data.value.schemaData.schema)
//      println(ce.data.value.schemaData.schema)
//    }
//  }
//
//}
//
//object MapDomainSpec {
//  implicit val avronautDurationDecoder: Decoder[Duration] = new Decoder[Duration] {
//    override def apply[B](value: B, failFast: Boolean): Results[Duration] =
//      Decoder.longDecoder.apply(value, failFast).map(Duration.ofMillis(_))
//  }
//
//  implicit val durationEncoder: EncoderBuilder[Duration] = new EncoderBuilder[Duration] {
//    override type Ret = Long
//
//    override def apply(value: Duration, schemaData: SchemaData, failFast: Boolean): Long =
//      Long.box(value.toMillis)
//  }
//
//  implicit val avronautDurationSchema: SchemaBuilder[Duration] = new SchemaBuilder[Duration] {
//    override def schema: Results[AvroSchemaADT] = Right(SchemaLong)
//  }
//
//}
