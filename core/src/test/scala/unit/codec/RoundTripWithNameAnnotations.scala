package unit.codec

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.Codec
import com.rauchenberg.avronaut.common.annotations.SchemaAnnotations.{Name, Namespace, SchemaMetadata}
import unit.common.RunRoundTripAssert._
import unit.utils.UnitSpecBase

class RoundTripWithNameAnnotations extends UnitSpecBase {

  "encoding and decoding" should {
    "respect annotations which change names and namespaces" in {
      runCodecRoundTrip[RecordWithNameAnnotation]
      runCodecRoundTrip[RecordWithNamespaceAnnotation]
    }
  }

  case class RecordWithNameAnnotation(@SchemaMetadata(Map(Name           -> "cupcat")) field1: Set[Int])
  case class RecordWithNamespaceAnnotation(@SchemaMetadata(Map(Namespace -> "com.rauchenberg")) field1: Set[Int])
  implicit val recordWithNameAnnotationCodec: Codec[RecordWithNameAnnotation] = Codec[RecordWithNameAnnotation]
  implicit val recordWithNamespaceAnnotationCodec: Codec[RecordWithNamespaceAnnotation] =
    Codec[RecordWithNamespaceAnnotation]
}
