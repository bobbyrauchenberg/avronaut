package unit.decoder.utils

import cats.scalatest.EitherMatchers
import com.rauchenberg.cupcatAvro.decoder.{DecodeTo, Decoder}
import com.rauchenberg.cupcatAvro.schema.AvroSchema
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.scalatest.Matchers
import collection.JavaConverters._

object RunAssert extends Matchers with EitherMatchers {

  def singleFieldAssertion[T, U: Decoder : AvroSchema](fieldValue: T, expected: U) = {

    val schema = AvroSchema[U].schema.right.get
    val field = new Schema.Field("field", schema)
    val recordSchema = Schema.createRecord(List(field).asJava)
    val record = new GenericData.Record(recordSchema)
    record.put("field", fieldValue)

    DecodeTo[U](record) should beRight(expected)
  }

}
