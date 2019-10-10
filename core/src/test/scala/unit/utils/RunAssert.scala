package unit.utils

import cats.scalatest.{EitherMatchers, EitherValues}
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecordBuilder}
import org.scalatest.Matchers

import scala.collection.JavaConverters._

object RunAssert extends Matchers with EitherMatchers with EitherValues {

  def runAssert[A, B : Decoder : AvroSchema](fieldValue: A, expected: B) = {

    val schemaData = AvroSchema.toSchema[B].value

    val record = new GenericData.Record(schemaData.schema)
    record.put("field", fieldValue)

    Decoder.decode[B](record, schemaData) should beRight(expected)
  }

  def runListAssert[A, B : Decoder : AvroSchema](fieldValue: Seq[A], expected: B) = {

    val schemaData = AvroSchema.toSchema[B].value

    val recordBuilder = new GenericRecordBuilder(schemaData.schema)
    recordBuilder.set("field", fieldValue.asJava)

    Decoder.decode[B](recordBuilder.build(), schemaData) should beRight(expected)
  }
}
