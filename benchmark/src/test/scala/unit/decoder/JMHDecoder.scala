package unit.decoder

import java.util.concurrent.TimeUnit

import com.rauchenberg.avronaut.common.Result
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.schema.AvroSchema
import com.sksamuel.avro4s.{DefaultFieldMapper, SchemaFor, Decoder => Decoder4s}
import org.openjdk.jmh.annotations._

trait NestedRecordDecoding extends DecoderBenchmarkNestedRecordData {

  val avroSchema = AvroSchema.toSchema[RecordWithNestedCaseClasses]
  val decoder    = Decoder[RecordWithNestedCaseClasses]
  @Benchmark
  def runNestedDecoder: List[Result[RecordWithNestedCaseClasses]] =
    dataSet.map { element =>
      Decoder.decode[RecordWithNestedCaseClasses](element, decoder)
    }
}

trait NestedAvro4SRecordDecoding extends DecoderBenchmarkNestedRecordData {

  val outerSchema          = SchemaFor[RecordWithNestedCaseClasses].schema(DefaultFieldMapper)
  implicit val decoder     = Decoder4s[RecordWithNestedCaseClasses]
  implicit val fieldMapper = DefaultFieldMapper

  @Benchmark
  def runNestedDecoder: List[RecordWithNestedCaseClasses] =
    dataSet.map { element =>
      decoder.decode(element, outerSchema, DefaultFieldMapper)
    }
}

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class AvronautNestedDecodingBenchmark extends NestedRecordDecoding

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class NestedAvro4SDecodingBenchmark extends NestedAvro4SRecordDecoding
