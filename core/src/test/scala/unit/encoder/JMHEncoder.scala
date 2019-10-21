package unit.encoder

import java.util.concurrent.TimeUnit

import com.rauchenberg.avronaut.common.Result
import com.rauchenberg.avronaut.encoder.Encoder
import com.sksamuel.avro4s.{DefaultFieldMapper, Encoder => Avro4SEncoder}
import org.apache.avro.generic.GenericData
import org.openjdk.jmh.annotations._

trait AvronautEncodingManyStrings extends EncoderBenchmarkDataManyStrings {
  implicit val encoder = Encoder[RecordWithNestedCaseClasses]
  implicit val sd      = writerSchema.data

  val data = testData

  @Benchmark
  def runNestedEncoder: List[Result[GenericData.Record]] =
    data.map { element =>
      Encoder.encode[RecordWithNestedCaseClasses](element)
    }
}

trait Avro4SEncodingManyStrings extends EncoderBenchmarkDataManyStrings {
  implicit val encoder = Avro4SEncoder[RecordWithNestedCaseClasses]
  val schema           = writerSchema.data.right.get.schema

  val data = testData

  @Benchmark
  def runNestedEncoder: List[AnyRef] =
    data.map { element =>
      encoder.encode(element, schema, DefaultFieldMapper)
    }
}

trait AvronautEncodingNoStrings extends EncoderBenchmarkDataNoStrings {
  implicit val encoder = Encoder[RecordWithNestedCaseClasses]
  implicit val sd      = writerSchema.data

  val data = testData

  @Benchmark
  def runNestedEncoder: List[Result[GenericData.Record]] =
    data.map { element =>
      Encoder.encode[RecordWithNestedCaseClasses](element)
    }
}

trait Avro4SRecordEncodingNoStrings extends EncoderBenchmarkDataNoStrings {
  implicit val encoder = Avro4SEncoder[RecordWithNestedCaseClasses]
  val schema           = writerSchema.data.right.get.schema

  val data = testData

  @Benchmark
  def runNestedEncoder: List[AnyRef] =
    data.map { element =>
      encoder.encode(element, schema, DefaultFieldMapper)
    }
}

trait AvronautSimpleRecord extends EncoderBenchmarkSimpleRecord {
  implicit val encoder = Encoder[SimpleRecord]
  implicit val sd      = writerSchema.data

  val data = testData

  @Benchmark
  def runNestedEncoder: List[Result[GenericData.Record]] =
    data.map { element =>
      Encoder.encode[SimpleRecord](element)
    }
}

trait Avro4SSimpleRecord extends EncoderBenchmarkSimpleRecord {
  implicit val encoder = Avro4SEncoder[SimpleRecord]
  val schema           = writerSchema.data.right.get.schema

  val data = testData

  @Benchmark
  def runNestedEncoder: List[Any] =
    data.map { element =>
      encoder.encode(element, schema, DefaultFieldMapper)
    }
}

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class AvronautNestedEncodingBenchmarkManyStrings extends AvronautEncodingManyStrings

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class Avro4SNestedEncodingBenchmarkManyStrings extends Avro4SEncodingManyStrings

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class AvronautNestedEncodingBenchmarkNoStrings extends AvronautEncodingNoStrings

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class Avro4SNestedEncodingBenchmarkNoStrings extends Avro4SRecordEncodingNoStrings

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class AvronautEncodingBenchmarkSimpleRecord extends AvronautSimpleRecord

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class Avro4SEncodingBenchmarkSimpleRecord extends Avro4SSimpleRecord
