package unit.schema

import java.util.UUID

import com.rauchenberg.avronaut.common.safe
import com.rauchenberg.avronaut.schema.{AvroSchema, SchemaResult}
import org.apache.avro.SchemaBuilder

case class UserDefinedInstance(value: Boolean, u: UUID)

object UserDefinedInstance {
  implicit val sb = new AvroSchema[UUID] {
    override def schema: SchemaResult = safe(SchemaBuilder.builder.stringType)
  }

}

case class OverrideInt(value: Int)

object OverrideInt {
  implicit val sb = new AvroSchema[Int] {
    override def schema: SchemaResult =
      safe(SchemaBuilder.builder.stringType)
  }
}
