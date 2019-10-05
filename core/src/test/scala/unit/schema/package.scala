package unit

import com.rauchenberg.avronaut.schema.AvroSchema

package object schema {

  def schemaAsString[A : AvroSchema] = AvroSchema[A].schema.map(_.toString)

}
