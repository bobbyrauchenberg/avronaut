package unit.utils

import cats.scalatest.{EitherMatchers, EitherValues}
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecordBuilder}
import org.scalatest.Matchers

import scala.collection.JavaConverters._

object RunAssert extends Matchers with EitherMatchers with EitherValues {

  def runAssert[A, B : Decoder : AvroSchema](fieldValue: A, expected: B) = {

    val schema = AvroSchema[B].schema

    val record = new GenericData.Record(schema.value)
    record.put("field", fieldValue)

    Decoder.decode[B](record) should beRight(expected)
  }

  def runListAssert[A, B : Decoder : AvroSchema](fieldValue: Seq[A], expected: B) = {

    val schema = AvroSchema[B].schema

    val rootRecord = new GenericData.Record(schema.value)

    val recordBuilder = new GenericRecordBuilder(rootRecord)
    recordBuilder.set("field", fieldValue.asJava)

    Decoder.decode[B](recordBuilder.build()) should beRight(expected)
  }
}
