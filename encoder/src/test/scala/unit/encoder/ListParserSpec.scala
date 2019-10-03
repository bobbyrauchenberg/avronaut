//package unit.encoder
//
//import collection.JavaConverters._
//import com.rauchenberg.avronaut.common.{AvroArray, AvroNumLong, AvroNumber}
//import com.rauchenberg.avronaut.encoder.ListParser
//import com.rauchenberg.avronaut.schema.AvroSchema
//import unit.common.UnitSpecBase
//
//class ListParserSpec extends UnitSpecBase {
//
//  "list parser" should {
//    "handle a basic list" in {
//
//      val schema = AvroSchema[SimpleList].schema.value
//
//      val arraySchema = schema.getFields.asScala(0).schema()
//
//      val list = AvroArray(List(AvroNumber(AvroNumLong(123)), AvroNumber(AvroNumLong(456))))
//
//      println(ListParser(arraySchema, list).parse)
////      ListParser(arraySchema, list).parse.map(_.asScala.toList) should beRight(List(123, 456).asInstanceOf[List[Any]])
//    }
////
//    "handle a nested list" in {
//
//      val schema = AvroSchema[NestedList].schema.value
//
//      val arraySchema = schema.getFields.asScala(0).schema()
//
//      val list =
//        AvroArray(List(AvroArray(List(AvroNumber(AvroNumLong(123)))), AvroArray(List(AvroNumber(AvroNumLong(456))))))
//
//      println(ListParser(arraySchema, list).parse)
//    }
//
//    import shapeless._
//    "blah" in {
//      type CP = Int :+: Boolean :+: CNil
//
//      val s = Coproduct[CP](123)
//      val b = Coproduct[CP](true)
//
//      object folder extends Poly1 {
//        implicit def caseInt = at[Int] { i =>
//          i
//        }
//        implicit def caseBool = at[Boolean](i => i)
//      }
//
//      val res = List(s, b).map { cp =>
//        cp.fold(folder)
//      }
//
//      println(res)
//
//    }
//
//  }
//
//  case class SimpleList(field: List[Long])
//  case class NestedList(field: List[List[Long]])
//
//  case class Nested(field: Int)
//  case class ListWithCaseClass(field: List[Nested])
//
//}
