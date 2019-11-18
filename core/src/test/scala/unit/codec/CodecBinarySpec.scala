package unit.codec

import java.time.OffsetDateTime

import com.danielasfregola.randomdatagenerator.RandomDataGenerator._
import com.rauchenberg.avronaut.Codec
import unit.utils.UnitSpecBase

class CodecBinarySpec extends UnitSpecBase {

  "codec should be able to encode a binary" in {

//    val r = Record("cupcat", List("cup", "cat", "234"), true)

//    implicit val codec          = Codec[Record]
    implicit val codecWithBytes = Codec[RecordWithBytes]

    val withBytes = RecordWithBytes(OffsetDateTime.now(),
                                    "cupcat",
                                    "cup".getBytes,
                                    Option("cat".getBytes),
                                    Map("cupcat" -> "cupcat".getBytes))

//    Codec.toBinary(r).map { bytes =>
//      println(new String(bytes))
//      println(Codec.fromBinary[Record](bytes))
//    }
    println(Codec.toBinary(withBytes))
    Codec.toBinary(withBytes).map { bytes =>
      println(new String(bytes))
      println("1!!!!!" + Codec.fromBinary[RecordWithBytes](bytes))
    }

//    implicit val schema  = AvroSchema[Record]
//    implicit val encoder = Encoder[Record]
//
//    val baos   = new ByteArrayOutputStream()
//    val output = AvroOutputStream.binary[Record].to(baos).build(schema)
//    output.write(r)
//
    //    output.close()
//    println(new String(baos.toByteArray))
  }

  case class Record(field1: String, field2: List[String], field3: Boolean)

  case class RecordWithBytes(time: OffsetDateTime,
                             topic: String,
                             key: Array[Byte],
                             value: Option[Array[Byte]],
                             headers: Map[String, Array[Byte]])

}
