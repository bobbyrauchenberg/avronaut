package unit.decoder

import java.util.concurrent.TimeUnit

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator
import com.rauchenberg.avronaut.common.Result
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.schema.AvroSchema
import com.sksamuel.avro4s.{DefaultFieldMapper, FromRecord, SchemaFor, Decoder => Decoder4s}
import org.apache.avro.generic.GenericData
import org.openjdk.jmh.annotations._

case class RecordWithMultipleFields(field1: Boolean, field2: String, field3: Int)
class SimpleRecordData extends RandomDataGenerator {

  val writerSchema = AvroSchema.toSchema[RecordWithMultipleFields].right.get
  val decoder      = Decoder[RecordWithMultipleFields]
  def prepare: List[(GenericData.Record, RecordWithMultipleFields)] =
    (1 to 1000).toList.flatMap { _ =>
      val data   = random[RecordWithMultipleFields]
      val record = new GenericData.Record(writerSchema.schema)
      record.put(0, data.field1)
      record.put(1, data.field2)
      record.put(2, data.field3)
      List((record, data))
    }
  val testData = prepare

}

class SimpleRecordDataAvro4s extends RandomDataGenerator {

  implicit val writerSchema = SchemaFor[RecordWithMultipleFields].schema(DefaultFieldMapper)
  implicit val decoder      = Decoder4s[RecordWithMultipleFields]

  def prepare: List[(GenericData.Record, RecordWithMultipleFields)] = {
    implicit val writerSchema = AvroSchema.toSchema[RecordWithMultipleFields].right.get
    (1 to 100).map { _ =>
      val data   = random[RecordWithMultipleFields]
      val record = new GenericData.Record(writerSchema.schema)
      record.put(0, data.field1)
      record.put(1, data.field2)
      record.put(2, data.field3)
      (record, data)
    }.toList
  }

  val testData = prepare
}

class NestedRecordData extends RandomDataGenerator {

  case class InnerNested(field1: String, field2: Int)
  case class Nested(field1: String, field2: InnerNested, field3: Int)
  case class RecordWithNestedCaseClasses(field: Nested)

  val writerSchema = AvroSchema.toSchema[RecordWithNestedCaseClasses].right.get
  val nestedSchema = AvroSchema.toSchema[Nested].right.get
  val innerSchema  = AvroSchema.toSchema[InnerNested].right.get

  val decoder = Decoder[RecordWithNestedCaseClasses]

  def prepare: List[(GenericData.Record, RecordWithNestedCaseClasses)] = {
    (1 to 100).flatMap { _ =>
      val data   = random[RecordWithNestedCaseClasses]
      val record = new GenericData.Record(writerSchema.schema)
      val nested = new GenericData.Record(nestedSchema.schema)
      val inner  = new GenericData.Record(innerSchema.schema)

      inner.put(0, data.field.field2.field1)
      inner.put(1, data.field.field2.field2)
      nested.put(0, data.field.field1)
      nested.put(1, inner)
      nested.put(2, data.field.field3)
      record.put(0, nested)
      List((record, data))
    }
  }.toList

  val testData = prepare
}

class NestedAvro4SRecordData extends RandomDataGenerator {

  case class InnerNested(field1: String, field2: Int)
  case class Nested(field1: String, field2: InnerNested, field3: Int)
  case class RecordWithNestedCaseClasses(field: Nested)

  implicit val outerSchema  = SchemaFor[RecordWithNestedCaseClasses].schema(DefaultFieldMapper)
  implicit val nestedSchema = SchemaFor[Nested].schema(DefaultFieldMapper)
  implicit val innerSchema  = SchemaFor[InnerNested].schema(DefaultFieldMapper)

  val decoder = Decoder[RecordWithNestedCaseClasses]

  def prepare: List[(GenericData.Record, RecordWithNestedCaseClasses)] = {
    implicit val writerSchema = AvroSchema.toSchema[RecordWithNestedCaseClasses].right.get
    (1 to 100).map { _ =>
      val data   = random[RecordWithNestedCaseClasses]
      val record = new GenericData.Record(writerSchema.schema)
      val nested = new GenericData.Record(nestedSchema)
      val inner  = new GenericData.Record(innerSchema)

      inner.put(0, data.field.field2.field1)
      inner.put(1, data.field.field2.field2)
      nested.put(0, data.field.field1)
      nested.put(1, inner)
      nested.put(2, data.field.field3)
      record.put(0, nested)
      (record, data)
    }.toList
  }

  val testData = prepare
}

trait SimpleRecordDecoding { self: SimpleRecordData =>
  @Benchmark
  def runDecoder: List[Result[RecordWithMultipleFields]] =
    testData.map { element =>
      Decoder.decode[RecordWithMultipleFields](element._1, writerSchema)(decoder)
    }
}

trait Avro4SSimpleRecordDecoding { self: SimpleRecordDataAvro4s =>
  @Benchmark
  def runAvro4sDecoder: List[RecordWithMultipleFields] =
    testData.map { element =>
      FromRecord(writerSchema).from(element._1)
    }
}

trait NestedRecordDecoding { self: NestedRecordData =>
  @Benchmark
  def runNestedDecoder: List[Result[RecordWithNestedCaseClasses]] =
    testData.map { element =>
      Decoder.decode[RecordWithNestedCaseClasses](element._1, writerSchema)(decoder)
    }
}

trait NestedAvro4SRecordDecoding { self: NestedAvro4SRecordData =>
  @Benchmark
  def runNestedDecoder: List[RecordWithNestedCaseClasses] =
    testData.map { element =>
      FromRecord[RecordWithNestedCaseClasses](outerSchema).from(element._1)
    }
}
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class DecodingBenchmark extends SimpleRecordData with SimpleRecordDecoding

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class Avro4SDecodingBenchmark extends SimpleRecordDataAvro4s with Avro4SSimpleRecordDecoding

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class NestedDecodingBenchmark extends NestedRecordData with NestedRecordDecoding

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class NestedAvro4SDecodingBenchmark extends NestedRecordData with NestedRecordDecoding
