package unit.encoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator
import com.rauchenberg.avronaut.schema.AvroSchema
import org.scalacheck.{Arbitrary, Gen}

trait EncoderBenchmarkDataManyStrings extends RandomDataGenerator {

  import unit.common.SizedArbitraries._

  implicit val writerSchema = AvroSchema.toSchema[RecordWithNestedCaseClasses]

  val genChar: Gen[Char]                     = Gen.alphaChar
  val genSizedStr: Gen[String]               = stringOfN(30)(Arbitrary(genChar))
  val genStrList: Gen[List[String]]          = listOfN[String](15)(genSizedStr)
  val genIntList: Gen[List[Int]]             = listOfN[Int](15)(Gen.posNum[Int])
  val genIntListField3: Gen[List[Int]]       = listOfN[Int](15)(Gen.posNum[Int])
  val nestedStrList: Gen[List[List[String]]] = listOfN[List[String]](10)(genStrList)
  val nestedIntList: Gen[List[List[Int]]]    = listOfN[List[Int]](10)(genIntList)

  val innerNested: Gen[List[List[InnerNested]]] = for {
    sl  <- nestedStrList
    si  <- nestedIntList
    in  <- Gen.const(InnerNested(sl, si))
    ln  <- listOfN[InnerNested](10)(in)
    lln <- listOfN[List[InnerNested]](10)(ln)
  } yield lln

  implicit val myGen: Arbitrary[RecordWithNestedCaseClasses] = Arbitrary(for {
    field1String  <- stringOfN(10)
    field3IntList <- genIntListField3
    innerNested   <- innerNested
    nested        <- Gen.const(Nested(field1String, innerNested, field3IntList))
  } yield RecordWithNestedCaseClasses(nested))

  case class InnerNested(field1: List[List[String]], field2: List[List[Int]])
  case class Nested(field1: String, field2: List[List[InnerNested]], field3: List[Int])
  case class RecordWithNestedCaseClasses(field: Nested)

  def testData: List[RecordWithNestedCaseClasses] = {
    (1 to 100).map { _ =>
      random[RecordWithNestedCaseClasses]
    }
  }.toList

  val dataSet = testData
}

trait EncoderBenchmarkDataNoStrings extends RandomDataGenerator {

  import unit.common.SizedArbitraries._

  implicit val writerSchema = AvroSchema.toSchema[RecordWithNestedCaseClasses]

  val genChar: Gen[Char]                  = Gen.alphaChar
  val genIntList: Gen[List[Int]]          = listOfN[Int](30)(Gen.posNum[Int])
  val genIntListField: Gen[List[Int]]     = listOfN[Int](5000)(Gen.posNum[Int])
  val nestedIntList: Gen[List[List[Int]]] = listOfN[List[Int]](20)(genIntList)

  val innerNested: Gen[List[List[InnerNested]]] = for {
    si  <- nestedIntList
    in  <- Gen.const(InnerNested(si))
    ln  <- listOfN[InnerNested](10)(in)
    lln <- listOfN[List[InnerNested]](10)(ln)
  } yield lln

  implicit val myGen: Arbitrary[RecordWithNestedCaseClasses] = Arbitrary(for {
    field2IntList <- genIntListField
    innerNested   <- innerNested
    nested        <- Gen.const(Nested(innerNested, field2IntList))
  } yield RecordWithNestedCaseClasses(nested))

  case class InnerNested(field1: List[List[Int]])
  case class Nested(field1: List[List[InnerNested]], field2: List[Int])
  case class RecordWithNestedCaseClasses(field: Nested)

  def testData: List[RecordWithNestedCaseClasses] = {
    (1 to 100).map { _ =>
      random[RecordWithNestedCaseClasses]
    }
  }.toList

  val dataSet = testData

}

trait EncoderBenchmarkSimpleRecord extends RandomDataGenerator {

  import unit.common.SizedArbitraries._

  implicit val stringSize: Arbitrary[String] = Arbitrary(stringOfN(10))

  case class SimpleRecord(s: String, i: Int, b: Boolean, f: Float, d: Double, l: Long)

  implicit val writerSchema = AvroSchema.toSchema[SimpleRecord]

  def testData: List[SimpleRecord] =
    (1 to 1000).map { _ =>
      random[SimpleRecord]
    }.toList

  val dataSet = testData
}
