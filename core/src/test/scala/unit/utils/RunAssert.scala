package unit.utils

import cats.scalatest.{EitherMatchers, EitherValues}
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.{AvroSchema, SchemaBuilder}
import org.apache.avro.generic.{GenericData, GenericRecordBuilder}
import org.scalatest.Matchers

import scala.collection.JavaConverters._

object RunAssert extends Matchers with EitherMatchers with EitherValues {

  def runDecodeAssert[A, B : Decoder](fieldValue: A, expected: B)(implicit schema: AvroSchema[B]) = {

    val record = new GenericData.Record(schema.data.value.schema)
    record.put("field", fieldValue)

    Decoder.decode[B](record) should beRight(expected)
  }

  def runEncodeAssert[A : Encoder](value: A, expected: GenericData.Record)(implicit schema: AvroSchema[A]) =
    Encoder.encode(value) should beRight(expected)

  def runListAssert[A, B : Decoder : SchemaBuilder](fieldValue: Seq[A], expected: B)(implicit schema: AvroSchema[B]) = {

    val recordBuilder = new GenericRecordBuilder(schema.data.value.schema)
    recordBuilder.set("field", fieldValue.asJava)

    Decoder.decode[B](recordBuilder.build) should beRight(expected)
  }
}
