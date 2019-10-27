package unit.utils

import cats.scalatest.{EitherMatchers, EitherValues}
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecord, GenericRecordBuilder}
import org.scalatest.Matchers

import scala.collection.JavaConverters._

object RunAssert extends Matchers with EitherMatchers with EitherValues {

  def runDecodeAssert[A, B](fieldValue: A, expected: B)(implicit schema: AvroSchema[B], decoder: Decoder[B]) = {

    val record = new GenericData.Record(schema.data.value.schema)
    record.put("field", fieldValue)

    Decoder.decode[B](record, decoder) should beRight(expected)
  }

  def runEncodeAssert[A](value: A, expected: GenericRecord)(implicit encoder: Encoder[A], schema: AvroSchema[A]) =
    Encoder.encode(value, encoder, schema.data) should beRight(expected)

  def runListAssert[A, B](fieldValue: Seq[A], expected: B)(implicit
                                                           schema: AvroSchema[B],
                                                           decoder: Decoder[B]) = {

    val recordBuilder = new GenericRecordBuilder(schema.data.value.schema)
    recordBuilder.set("field", fieldValue.asJava)

    Decoder.decode[B](recordBuilder.build, decoder) should beRight(expected)
  }
}
