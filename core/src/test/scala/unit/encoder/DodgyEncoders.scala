package unit.encoder

import com.rauchenberg.avronaut.common.{Error, Result}
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.SchemaData

object DodgyEncoders {
  implicit val intEncoder = new Encoder[Int] {
    override type Ret = Int

    override def apply(value: Int, schemaData: SchemaData, failFast: Boolean): Int = throw new Exception("int blew up")
  }

  implicit val boolEncoder = new Encoder[Boolean] {
    override type Ret = Result[Boolean]

    override def apply(value: Boolean, schemaData: SchemaData, failFast: Boolean): Result[Boolean] =
      Left(Error("boolean blew up"))
  }
}