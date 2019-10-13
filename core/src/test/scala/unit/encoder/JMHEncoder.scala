package unit.encoder

import java.util.concurrent.TimeUnit

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator
import com.rauchenberg.avronaut.common.Result
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import com.sksamuel.avro4s.{DefaultFieldMapper, Encoder => Avro4SEncoder}
import org.apache.avro.generic.GenericData
import org.openjdk.jmh.annotations._

class NestedRecordData extends RandomDataGenerator {

  case class InnerNested(field1: List[List[String]], field2: List[List[Int]])
  case class Nested(field1: String, field2: List[List[InnerNested]], field3: Int)
  case class RecordWithNestedCaseClasses(field: Nested)

  val writerSchema = AvroSchema.toSchema[RecordWithNestedCaseClasses].right.get

  val encoder = Encoder[RecordWithNestedCaseClasses]

  def prepare: List[RecordWithNestedCaseClasses] = {
    (1 to 100).map { _ =>
      random[RecordWithNestedCaseClasses]
    }
  }.toList

  val testData = prepare
}

class NestedAvro4SRecordData extends RandomDataGenerator {

  case class InnerNested(field1: List[List[String]], field2: List[List[Int]])
  case class Nested(field1: String, field2: List[List[InnerNested]], field3: Int)
  case class RecordWithNestedCaseClasses(field: Nested)

  val writerSchema = AvroSchema.toSchema[RecordWithNestedCaseClasses].right.get.schema

  val encoder = Avro4SEncoder[RecordWithNestedCaseClasses]

  def prepare: List[RecordWithNestedCaseClasses] = {
    (1 to 100).map { _ =>
      random[RecordWithNestedCaseClasses]
    }
  }.toList

  val testData = prepare
}

trait NestedRecordEncoding { self: NestedRecordData =>
  @Benchmark
  def runNestedEncoder: List[Result[GenericData.Record]] =
    testData.map { element =>
      Encoder.encode[RecordWithNestedCaseClasses](element, writerSchema)(encoder)
    }
}

trait NestedAvro4SRecordEncoding { self: NestedAvro4SRecordData =>
  @Benchmark
  def runNestedEncoder: List[AnyRef] =
    testData.map { element =>
      encoder.encode(element, writerSchema, DefaultFieldMapper)
    }
}

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class NestedEncodingBenchmark extends NestedRecordData with NestedRecordEncoding

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class NestedAvro4SEncodingBenchmark extends NestedAvro4SRecordData with NestedAvro4SRecordEncoding
