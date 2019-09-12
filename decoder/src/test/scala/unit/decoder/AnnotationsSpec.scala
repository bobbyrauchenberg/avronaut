package unit.decoder

import com.rauchenberg.cupcatAvro.common.annotations.SchemaAnnotations.{Name, SchemaMetadata}
import com.rauchenberg.cupcatAvro.decoder.{DecodeTo, Decoder}
import com.rauchenberg.cupcatAvro.schema.AvroSchema
import org.apache.avro.generic.GenericData
import unit.common.UnitSpecBase

class AnnotationsSpec extends UnitSpecBase {

  "decoder" should {
    "decode a record generated from a case class with a name annotation on a field" in new TestContext {
      runAssert[NameAnnotation]("cupcat", NameAnnotation("cupcat"))
    }
  }

  "decode a caseclass with a sealed trait enum with a name annotation" in new TestContext {
    runAssert[AnnotatedEnum](AnnotatedCat.toString, AnnotatedEnum(AnnotatedCat))
  }

  trait TestContext {
    def runAssert[T : AvroSchema : Decoder](recordValue: String, expected: T) = {
      val schema = AvroSchema[T].schema.value

      val record = new GenericData.Record(schema)
      record.put("cupcat", recordValue)

      DecodeTo[T](record) should beRight(expected)
    }
  }

  case class NameAnnotation(@SchemaMetadata(Map(Name -> "cupcat")) field: String)
  case class AnnotatedEnum(@SchemaMetadata(Map(Name  -> "cupcat")) field: Annotated)
}

sealed trait Annotated
case object AnnotatedCup extends Annotated
case object AnnotatedCat extends Annotated
