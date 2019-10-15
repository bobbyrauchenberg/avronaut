package unit

import cats.scalatest.EitherValues
import com.rauchenberg.avronaut.schema.AvroSchema

package object schema extends EitherValues {

  def schemaAsString[A](implicit schema: AvroSchema[A]) = schema.data.value.schema.toString

}
