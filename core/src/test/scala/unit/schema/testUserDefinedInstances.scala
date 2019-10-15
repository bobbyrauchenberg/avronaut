package unit.schema

import java.util.UUID

import cats.syntax.either._
import com.rauchenberg.avronaut.common.Result
import com.rauchenberg.avronaut.schema.{AvroSchema, SchemaData}
import org.apache.avro.{Schema, SchemaBuilder}

case class UserDefinedInstance(value: Boolean, u: UUID)

case class OverrideInt(value: Int)

object OverrideInt {
  implicit val sb = new AvroSchema[Int] {
    override def data: Result[SchemaData] =
      SchemaData(Map.empty[String, Schema], SchemaBuilder.builder.stringType).asRight
  }
}
