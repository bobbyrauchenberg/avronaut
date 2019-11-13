package unit.utils

import cats.scalatest.{EitherMatchers, EitherValues}
import com.rauchenberg.avronaut.Codec
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecord, GenericRecordBuilder}
import org.scalatest.Matchers
import Codec._

import scala.collection.JavaConverters._

object RunAssert extends Matchers with EitherMatchers with EitherValues {

  def runDecodeAssert[A, B](fieldValue: A, expected: B)(implicit codec: Codec[B]) = {

    val schema = Codec.schema[B].value
    val record = new GenericData.Record(schema)
    record.put("field", fieldValue)

    record.decode[B] should beRight(expected)
  }

  def runEncodeAssert[A](value: A, expected: GenericRecord)(implicit encoder: Encoder[A]) =
    Encoder.encode(value, encoder) should beRight(expected)

  def runListAssert[A, B](fieldValue: Seq[A], expected: B)(implicit
                                                           codec: Codec[B]) = {

    val schema        = Codec.schema[B].value
    val recordBuilder = new GenericRecordBuilder(schema)
    recordBuilder.set("field", fieldValue.asJava)
    recordBuilder.build.decode[B] should beRight(expected)
  }
}
