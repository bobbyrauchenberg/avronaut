package unit.decoder

import collection.JavaConverters._
import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecord, GenericRecordBuilder}
import org.scalacheck.{Arbitrary, Gen}

trait DecoderBenchmarkNestedRecordData extends RandomDataGenerator {

  import unit.common.SizedArbitraries._

  implicit val writerSchema = AvroSchema.toSchema[RecordWithNestedCaseClasses].data.right.get.schema
  implicit val nestedSchema = AvroSchema.toSchema[Nested].data.right.get.schema
  implicit val innerSchema  = AvroSchema.toSchema[InnerNested].data.right.get.schema

  val genChar: Gen[Char]            = Gen.alphaChar
  val genSizedStr: Gen[String]      = stringOfN(30)(Arbitrary(genChar))
  val genStrList: Gen[List[String]] = listOfN[String](15)(genSizedStr)
  val genIntList: Gen[List[Int]]    = listOfN[Int](15)(Gen.posNum[Int])

  val stringIntListPair: Gen[(String, List[Int])] = for {
    il  <- genIntList
    str <- genSizedStr
  } yield (str, il)

  val mapStringInts: Gen[Map[String, List[Int]]] = for {
    map <- listOfN[(String, List[Int])](1000)(stringIntListPair)
  } yield map.toMap

  val innerNested: Gen[List[InnerNested]] = for {
    sl <- stringOfN(30)(Arbitrary(genChar))
    si <- Gen.posNum[Int]
    in <- Gen.const(InnerNested(sl, si))
    ln <- listOfN[InnerNested](10)(in)
  } yield ln

  implicit val myGen: Arbitrary[RecordWithNestedCaseClasses] = Arbitrary(for {
    field1String  <- stringOfN(10)
    field3IntList <- Gen.posNum[Int]
    innerNested   <- innerNested
    nested        <- Gen.const(Nested(field1String, innerNested, field3IntList))
    intMap        <- mapStringInts
  } yield RecordWithNestedCaseClasses(nested, intMap))

  case class InnerNested(field1: String, field2: Int)
  case class Nested(field1: String, field2: List[InnerNested], field3: Int)
  case class RecordWithNestedCaseClasses(field: Nested, field2: Map[String, List[Int]])

  def testData: List[GenericRecord] = {
    (1 to 100).map { _ =>
      val data   = random[RecordWithNestedCaseClasses]
      val record = new GenericData.Record(writerSchema)
      val nested = new GenericRecordBuilder(nestedSchema)

      val innerNestedJavaList = data.field.field2.map { in =>
        val inner = new GenericData.Record(innerSchema)
        inner.put(0, in.field1)
        inner.put(1, in.field2)
        inner
      }.asJava

      nested.set("field1", data.field.field1)
      nested.set("field2", innerNestedJavaList)
      nested.set("field3", data.field.field3)

      record.put(0, nested.build)
      record.put(1, data.field2.asJava)
      record.asInstanceOf[GenericRecord]
    }
  }.toList

  val dataSet: List[GenericRecord] = testData

}
