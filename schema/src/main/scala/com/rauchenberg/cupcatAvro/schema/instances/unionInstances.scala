package com.rauchenberg.cupcatAvro.schema.instances

import cats.implicits._
import com.rauchenberg.cupcatAvro.schema.{AvroSchema, SchemaResult}
import org.apache.avro.{Schema, SchemaBuilder}
import shapeless.{:+:, CNil, Coproduct}
import com.rauchenberg.cupcatAvro.common._
import scala.collection.JavaConverters._

trait unionInstances {

  implicit def optionSchema[T](implicit elementSchema: AvroSchema[T]) = new AvroSchema[Option[T]] {
    override def schema: SchemaResult = elementSchema.schema.flatMap { es =>
      safe(Schema.createUnion(List(SchemaBuilder.builder.nullType, es).asJava))
    }
  }

  implicit def eitherSchema[L, R](implicit leftSchema: AvroSchema[L], rightSchema: AvroSchema[R]) = new AvroSchema[Either[L, R]] {
    override def schema: SchemaResult = leftSchema.schema.map2(rightSchema.schema){ (l, r) => safe(Schema.createUnion(List(l, r).asJava)) }.flatten
  }

  implicit def cnilSchema[H](implicit hSchema: AvroSchema[H]) = new AvroSchema[H :+: CNil] {
    override def schema: SchemaResult = hSchema.schema.flatMap(v => safe(Schema.createUnion(v)))

  }

  implicit def coproductSchema[H, T <: Coproduct](implicit hSchema: AvroSchema[H], tSchema: AvroSchema[T]) = new AvroSchema[H :+: T] {
    override def schema: SchemaResult =
      tSchema.schema.map2(hSchema.schema){ (l, r) =>
        safe(Schema.createUnion((l.getTypes.asScala.toList :+ r).asJava))
      }.flatten
  }

}
