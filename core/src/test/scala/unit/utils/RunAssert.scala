package unit.utils

import cats.scalatest.{EitherMatchers, EitherValues}
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecordBuilder}
import org.scalatest.Matchers

import scala.collection.JavaConverters._

object RunAssert extends Matchers with EitherMatchers with EitherValues {

  def runDecodeAssert[A, B : Decoder : AvroSchema](fieldValue: A, expected: B) = {

    val schema = AvroSchema.toSchema[B].value

    val record = new GenericData.Record(schema.schema)
    record.put("field", fieldValue)

    Decoder.decode[B](record, schema) should beRight(expected)
  }

  def runEncodeAssert[A : Encoder : AvroSchema](value: A, expected: GenericData.Record) = {

    val schema = AvroSchema.toSchema[A].value

    Encoder.encode(value, schema) should beRight(expected)
  }

  def runListAssert[A, B : Decoder : AvroSchema](fieldValue: Seq[A], expected: B) = {

    val schema = AvroSchema.toSchema[B].value

    val recordBuilder = new GenericRecordBuilder(schema.schema)
    recordBuilder.set("field", fieldValue.asJava)

    Decoder.decode[B](recordBuilder.build(), schema) should beRight(expected)
  }
}
