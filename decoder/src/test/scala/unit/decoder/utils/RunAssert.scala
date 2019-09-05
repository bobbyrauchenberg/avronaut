package unit.decoder.utils

import cats.scalatest.EitherMatchers
import com.rauchenberg.cupcatAvro.decoder.{DecodeTo, Decoder}
import com.rauchenberg.cupcatAvro.schema.AvroSchema
import org.apache.avro.generic.GenericData
import org.scalatest.Matchers

object RunAssert extends Matchers with EitherMatchers {

  def runAssert[T, U: Decoder : AvroSchema](fieldValue: T, expected: U) = {

    val schema = AvroSchema[U].schema.right.get
    val record = new GenericData.Record(schema)
    record.put("field", fieldValue)

    DecodeTo[U](record) should beRight(expected)
  }

}
