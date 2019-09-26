package unit.decoder.utils

import cats.scalatest.{EitherMatchers, EitherValues}
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecordBuilder}
import org.scalatest.Matchers

import scala.collection.JavaConverters._

object RunAssert extends Matchers with EitherMatchers with EitherValues {

  def runAssert[T, U : Decoder : AvroSchema](fieldValue: T, expected: U) = {

    val schema = AvroSchema[U].schema

    val record = new GenericData.Record(schema.value)
    record.put("field", fieldValue)

    Decoder.decode[U](schema.value, record) should beRight(expected)
  }

  def runListAssert[T, U : Decoder : AvroSchema](fieldValue: Seq[T], expected: U) = {

    val schema = AvroSchema[U].schema

    val rootRecord = new GenericData.Record(schema.value)

    val recordBuilder = new GenericRecordBuilder(rootRecord)
    recordBuilder.set("field", fieldValue.asJava)

    Decoder.decode[U](schema.value, recordBuilder.build()) should beRight(expected)
  }
}
