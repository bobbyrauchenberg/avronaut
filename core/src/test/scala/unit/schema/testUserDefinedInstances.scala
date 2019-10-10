package unit.schema

import java.util.UUID

import cats.syntax.either._
import com.rauchenberg.avronaut.common.Result
import com.rauchenberg.avronaut.schema.{AvroSchema, AvroSchemaADT, SchemaString}

case class UserDefinedInstance(value: Boolean, u: UUID)

case class OverrideInt(value: Int)

object OverrideInt {
  implicit val sb = new AvroSchema[Int] {
    override def schema: Result[AvroSchemaADT] = SchemaString.asRight
  }
}
