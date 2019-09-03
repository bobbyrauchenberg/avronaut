import com.rauchenberg.cupcatAvro.schema.AvroSchema


package object common {

  def schemaAsString[T: AvroSchema] = AvroSchema[T].schema.map(_.toString)

}
