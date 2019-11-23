package unit.codec

import java.time.OffsetDateTime

import com.danielasfregola.randomdatagenerator.RandomDataGenerator._
import com.rauchenberg.avronaut.Codec
import unit.utils.UnitSpecBase

class CodecBinarySpec extends UnitSpecBase {

  "codec should be able to encode a binary" in {

    val r = Record("cupcat", List("cup", "cat", "234"), true)

    implicit val codec          = Codec[Record]
    implicit val codecWithBytes = Codec[RecordWithBytes]

    RecordWithBytes(OffsetDateTime.now(),
                    "cupcat",
                    "cup".getBytes,
                    Option("cat".getBytes),
                    Map("cupcat" -> "cupcat".getBytes))

  }

  case class Record(field1: String, field2: List[String], field3: Boolean)

  case class RecordWithBytes(time: OffsetDateTime,
                             topic: String,
                             key: Array[Byte],
                             value: Option[Array[Byte]],
                             headers: Map[String, Array[Byte]])

}
