//package unit.encoder
//
//import com.rauchenberg.avronaut.encoder.Encoder2
//import com.rauchenberg.avronaut.schema.AvroSchema
//import unit.utils.UnitSpecBase
//
//class Encoder2Spec extends UnitSpecBase {
//
//  "encoder 2" should {
//
//    "blah1" in {
//      implicit val schema = AvroSchema.toSchema[Simple]
//      val encoder         = Encoder2[Simple]
//      val record          = Simple("cup", "cat")
//
//      println(Encoder2.encode(record, encoder))
//    }
//
//    "blah2" in {
//      implicit val schema = AvroSchema.toSchema[Simple2]
//      val encoder         = Encoder2[Simple2]
//      val record          = Simple2("cup", List("cat", "snoot", "rendal"))
//
//      println(Encoder2.encode(record, encoder))
//    }
//
//    "blah3" in {
//      implicit val schema = AvroSchema.toSchema[Simple3]
//      val encoder         = Encoder2[Simple3]
//      val record          = Simple3("cup", List(List("cat", "snoot", "rendal"), List("cat", "snoot", "rendal")))
//
//      println(Encoder2.encode(record, encoder))
//    }
//
//    "blah4" in {
//      implicit val schema = AvroSchema.toSchema[Simple4]
//      val encoder         = Encoder2[Simple4]
//      val record          = Simple4("cup", List(List(List(List("cat", "snoot", "rendal"), List("cat", "snoot", "rendal")))))
//
//      println(Encoder2.encode(record, encoder))
//    }
//
//    "blah5" in {
//      implicit val schema = AvroSchema.toSchema[Simple5]
//      val encoder         = Encoder2[Simple5]
//      val record          = Simple5(None, Option("cupcat"))
//
//      println(Encoder2.encode(record, encoder))
//    }
//
//    "blah6" in {
//      implicit val schema = AvroSchema.toSchema[Simple6]
//      val encoder         = Encoder2[Simple6]
//      val record          = Simple6(Left("cup"), Right("cat"))
//
//      println(Encoder2.encode(record, encoder))
//    }
//
//    "blah7" in {
//      implicit val schema = AvroSchema.toSchema[Simple7]
//      val encoder         = Encoder2[Simple7]
//      val record = Simple7(Simple2("cup", List("cup", "cat")),
//                           List(Simple2("cup", List("cup", "cat")), Simple2("cup", List("cup2", "cat2"))))
//
//      println(Encoder2.encode(record, encoder))
//    }
//  }
//
//  case class Simple(field1: String, field2: String)
//  case class Simple2(field1: String, field2: List[String])
//  case class Simple3(field1: String, field2: List[List[String]])
//  case class Simple4(field1: String, field2: List[List[List[List[String]]]])
//  case class Simple5(field1: Option[String], field2: Option[String])
//  case class Simple6(field1: Either[String, Int], field2: Either[Boolean, String])
//  case class Simple7(field1: Simple2, field2: List[Simple2])
//}
