//package unit.decoder
//
//import java.util.concurrent.TimeUnit
//
//import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator
//import com.rauchenberg.avronaut.common.Result
//import com.rauchenberg.avronaut.decoder.Decoder
//import com.rauchenberg.avronaut.schema.AvroSchema
//import com.sksamuel.avro4s.{DefaultFieldMapper, FromRecord, SchemaFor, Decoder => Decoder4s}
//import org.apache.avro.generic.GenericData
//import org.openjdk.jmh.annotations._
//
//case class RecordWithMultipleFields(field1: Boolean, field2: String, field3: Int)
//class SimpleRecordData extends RandomDataGenerator {
//
//  implicit val writerSchema = AvroSchema.toSchema[RecordWithMultipleFields]
//  val decoder               = Decoder[RecordWithMultipleFields]
//  def testData: List[(GenericData.Record, RecordWithMultipleFields)] =
//    (1 to 1000).toList.flatMap { _ =>
//      val data   = random[RecordWithMultipleFields]
//      val record = new GenericData.Record(writerSchema.data.right.get.schema)
//      record.put(0, data.field1)
//      record.put(1, data.field2)
//      record.put(2, data.field3)
//      List((record, data))
//    }
//  val dataSet = testData
//
//}
//
//class SimpleRecordDataAvro4s extends RandomDataGenerator {
//
//  implicit val writerSchema = SchemaFor[RecordWithMultipleFields].schema(DefaultFieldMapper)
//  implicit val decoder      = Decoder4s[RecordWithMultipleFields]
//
//  def testData: List[(GenericData.Record, RecordWithMultipleFields)] = {
//    implicit val writerSchema = AvroSchema.toSchema[RecordWithMultipleFields].data.right.get
//    (1 to 100).map { _ =>
//      val data   = random[RecordWithMultipleFields]
//      val record = new GenericData.Record(writerSchema.schema)
//      record.put(0, data.field1)
//      record.put(1, data.field2)
//      record.put(2, data.field3)
//      (record, data)
//    }.toList
//  }
//
//  val dataSet = testData
//}
//
//trait SimpleRecordDecoding { self: SimpleRecordData =>
//  @Benchmark
//  def runDecoder: List[Result[RecordWithMultipleFields]] =
//    dataSet.map { element =>
//      Decoder.decode[RecordWithMultipleFields](element._1)
//    }
//}
//
//trait Avro4SSimpleRecordDecoding { self: SimpleRecordDataAvro4s =>
//  @Benchmark
//  def runAvro4sDecoder: List[RecordWithMultipleFields] =
//    dataSet.map { element =>
//      FromRecord(writerSchema).from(element._1)
//    }
//}
//
//trait NestedRecordDecoding extends DecoderBenchmarkNestedRecordData {
//
//  implicit val avroSchema = AvroSchema.toSchema[RecordWithNestedCaseClasses]
//
//  @Benchmark
//  def runNestedDecoder: List[Result[RecordWithNestedCaseClasses]] =
//    dataSet.map { element =>
//      Decoder.decode[RecordWithNestedCaseClasses](element)
//    }
//}
//
//trait NestedAvro4SRecordDecoding extends DecoderBenchmarkNestedRecordData {
//
//  val outerSchema          = SchemaFor[RecordWithNestedCaseClasses].schema(DefaultFieldMapper)
//  implicit val decoder     = Decoder4s[RecordWithNestedCaseClasses]
//  implicit val fieldMapper = DefaultFieldMapper
//
//  @Benchmark
//  def runNestedDecoder: List[RecordWithNestedCaseClasses] =
//    dataSet.map { element =>
//      decoder.decode(element, outerSchema, DefaultFieldMapper)
//    }
//}
//
//@State(Scope.Thread)
//@BenchmarkMode(Array(Mode.Throughput))
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//class AvronautDecodingBenchmark extends SimpleRecordData with SimpleRecordDecoding
//
//@State(Scope.Thread)
//@BenchmarkMode(Array(Mode.Throughput))
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//class Avro4SDecodingBenchmark extends SimpleRecordDataAvro4s with Avro4SSimpleRecordDecoding
//
//@State(Scope.Thread)
//@BenchmarkMode(Array(Mode.Throughput))
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//class AvronautNestedDecodingBenchmark extends NestedRecordDecoding
//
//@State(Scope.Thread)
//@BenchmarkMode(Array(Mode.Throughput))
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//class NestedAvro4SDecodingBenchmark extends NestedAvro4SRecordDecoding
