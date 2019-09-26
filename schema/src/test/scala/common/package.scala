import com.rauchenberg.avronaut.schema.AvroSchema

package object common {

  def schemaAsString[A : AvroSchema] = AvroSchema[A].schema.map(_.toString)

}
