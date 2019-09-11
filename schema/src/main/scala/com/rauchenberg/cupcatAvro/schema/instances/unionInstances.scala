package com.rauchenberg.cupcatAvro.schema.instances

import cats.implicits._
import com.rauchenberg.cupcatAvro.schema.{AvroSchema, SchemaResult}
import org.apache.avro.{Schema, SchemaBuilder}
import shapeless.{:+:, CNil, Coproduct}
import com.rauchenberg.cupcatAvro.common._
import scala.collection.JavaConverters._

trait unionInstances {

  implicit def optionSchema[T](implicit elementSchema: AvroSchema[T]) = new AvroSchema[Option[T]] {
    override def schema: SchemaResult = {

      elementSchema.schema.flatMap { es =>
        es.getType match {
          case Schema.Type.UNION =>
            safe(Schema.createUnion(SchemaBuilder.builder.nullType +: es.getTypes.asScala: _*))
          case _ => safe(Schema.createUnion(List(SchemaBuilder.builder.nullType, es).asJava))
        }
      }
    }
  }

  implicit def eitherSchema[L, R](implicit leftSchema: AvroSchema[L], rightSchema: AvroSchema[R]) = new AvroSchema[Either[L, R]] {
    override def schema: SchemaResult = {
      leftSchema.schema.map2(rightSchema.schema){ (l, r) => {
        (l.getType, r.getType) match {
          case (Schema.Type.UNION, Schema.Type.UNION) => safe(Schema.createUnion((l.getTypes.asScala ++ r.getTypes.asScala): _*))
          case (Schema.Type.UNION, _) => safe(Schema.createUnion((l.getTypes.asScala :+ r): _*))
          case (_, Schema.Type.UNION) => safe(Schema.createUnion((r.getTypes.asScala :+ l): _*))
          case _ => safe(Schema.createUnion(List(l, r).asJava))
        }
      } }.flatten
    }
  }

  implicit def cnilSchema[H](implicit hSchema: AvroSchema[H]) = new AvroSchema[H :+: CNil] {
    override def schema: SchemaResult = hSchema.schema.flatMap(v => safe(Schema.createUnion(v)))

  }

  implicit def coproductSchema[H, T <: Coproduct](implicit hSchema: AvroSchema[H], tSchema: AvroSchema[T]) = new AvroSchema[H :+: T] {
    override def schema: SchemaResult =
      tSchema.schema.map2(hSchema.schema){ (l, r) =>
        if(r.getType == Schema.Type.UNION)
          safe(Schema.createUnion((l.getTypes.asScala.toList ++ r.getTypes.asScala).asJava))
        else safe(Schema.createUnion((l.getTypes.asScala.toList :+ r).asJava))
      }.flatten
  }

}
