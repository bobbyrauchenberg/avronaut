package com.rauchenberg.avronaut.schema

import cats.implicits._
import com.rauchenberg.avronaut.common.{safeL, Error, Results}
import com.rauchenberg.avronaut.schema.AvroSchemaF._
import com.rauchenberg.avronaut.schema.helpers.SchemaHelper.{moveDefaultToHead, transformDefault}
import japgolly.microlibs.recursion.Recursion._
import japgolly.microlibs.recursion.{FAlgebraM, FCoalgebraM}
import org.apache.avro.Schema.Type._
import org.apache.avro.{LogicalTypes, Schema, SchemaBuilder => AvroSchemaBuilder}
import shims._

import scala.collection.JavaConverters._

case class Parser(record: AvroSchemaADT) {

  type Registry = Map[String, Schema]

  val empty = Map.empty[String, Schema]

  def parse: Either[List[Error], SchemaData] =
    hyloM[Results, AvroSchemaF, AvroSchemaADT, Either[(Registry, Schema.Field), (Registry, Schema)]](
      toSchemaF,
      toAvroSchema)(record).flatMap {
      _.leftMap(f => List(Error(s"expected a Schema, got a field $f")))
    }.map(SchemaData.tupled(_))

  val toSchemaF: FCoalgebraM[Results, AvroSchemaF, AvroSchemaADT] = {
    case SchemaInt                                   => SchemaIntF.asRight
    case SchemaLong                                  => SchemaLongF.asRight
    case SchemaFloat                                 => SchemaFloatF.asRight
    case SchemaDouble                                => SchemaDoubleF.asRight
    case SchemaBoolean                               => SchemaBooleanF.asRight
    case SchemaString                                => SchemaStringF.asRight
    case SchemaNull                                  => SchemaNullF.asRight
    case SchemaBytes                                 => SchemaBytesF.asRight
    case SchemaUUID                                  => SchemaUUIDF.asRight
    case SchemaTimestampMillis                       => SchemaTimestampMillisF.asRight
    case SchemaEnum(name, namespace, doc, values)    => SchemaEnumF(name, namespace, doc, values).asRight
    case SchemaList(values)                          => SchemaListF(values).asRight
    case SchemaMap(values)                           => SchemaMapF(values).asRight
    case SchemaOption(value)                         => SchemaOptionF(value).asRight
    case SchemaCoproduct(values)                     => SchemaCoproductF(values).asRight
    case SchemaNamedField(name, doc, default, value) => SchemaNamedFieldF(name, doc, default, value).asRight
    case SchemaRecord(name, namespace, doc, values)  => SchemaRecordF(name, namespace, doc, values).asRight
  }

  val toAvroSchema: FAlgebraM[Results, AvroSchemaF, Either[(Registry, Schema.Field), (Registry, Schema)]] = {
    case SchemaIntF     => (empty, AvroSchemaBuilder.builder.intType).asRight.asRight
    case SchemaLongF    => (empty, AvroSchemaBuilder.builder.longType).asRight.asRight
    case SchemaDoubleF  => (empty, AvroSchemaBuilder.builder.doubleType).asRight.asRight
    case SchemaFloatF   => (empty, AvroSchemaBuilder.builder.floatType).asRight.asRight
    case SchemaBooleanF => (empty, AvroSchemaBuilder.builder.booleanType).asRight.asRight
    case SchemaStringF  => (empty, AvroSchemaBuilder.builder.stringType).asRight.asRight
    case SchemaNullF    => (empty, AvroSchemaBuilder.builder.nullType).asRight.asRight
    case SchemaBytesF   => (empty, AvroSchemaBuilder.builder.bytesType).asRight.asRight
    case SchemaUUIDF    => (empty, LogicalTypes.uuid.addToSchema(AvroSchemaBuilder.builder.stringType)).asRight.asRight
    case SchemaTimestampMillisF =>
      (empty, LogicalTypes.timestampMillis.addToSchema(AvroSchemaBuilder.builder.longType)).asRight.asRight
    case SchemaEnumF(name, namespace, doc, values) =>
      schemaEnum(name, namespace, doc, values).map(s => (empty -> s).asRight)
    case SchemaListF(value) =>
      value match {
        case Left((_, v))    => error("Array", v).asLeft
        case Right((reg, v)) => safeL(Schema.createArray(v)).map(schema => (reg -> schema).asRight)
      }
    case SchemaMapF(value) =>
      value match {
        case Left((_, v))    => error("Map", v).asLeft
        case Right((reg, v)) => safeL(Schema.createMap(v)).map(schema => (reg -> schema).asRight)
      }
    case SchemaOptionF(value) =>
      value match {
        case Left((_, v)) => error("Option", v).asLeft
        case Right((reg, s)) if (s.getType == UNION) =>
          safeL(
            Schema.createUnion(((AvroSchemaBuilder.builder.nullType +: s.getTypes.asScala.toList).toSet.toList): _*))
            .map(s => (reg -> s).asRight)
        case Right((reg, s)) =>
          safeL(Schema.createUnion(List(AvroSchemaBuilder.builder.nullType, s).asJava)).map(s => (reg, s).asRight)
      }
    case SchemaCoproductF(values) =>
      values.sequence.traverse { values =>
        val flattenNestedUnions = values.toList.flatMap {
          case (_, schema) =>
            if (schema.getType == UNION) schema.getTypes.asScala.toList else List(schema)
        }
        val updateRegistry = values.foldLeft(empty) {
          case (acc, (reg, _)) =>
            acc ++ reg
        }
        safeL(Schema.createUnion(flattenNestedUnions: _*)).map(v => (updateRegistry -> v))
      }
    case SchemaNamedFieldF(name, doc, Some(default), Right((registry, schema))) =>
      fieldWithDefault(name, doc, default, schema).map(v => (registry -> v).asLeft)
    case SchemaNamedFieldF(name, doc, None, Right((registry, schema))) =>
      schemaField(name, schema, doc).map(v => (registry -> v).asLeft)
    case SchemaNamedFieldF(_, _, _, Left((_, field))) =>
      List(Error(s"building a Field, expected a Schema, not Schema.Field ${field.schema}")).asLeft
    case SchemaRecordF(name, namespace, doc, values) =>
      values
        .map(_.swap)
        .sequence
        .map { v =>
          val schema = Schema.createRecord(name, doc.getOrElse(""), namespace, false, v.map(_._2).asJava)
          val updated = v.foldLeft(empty) {
            case (acc, (reg, _)) => acc ++ reg + (s"$namespace.$name" -> schema)
          }
          schema.asRight.map(v => (updated -> v))
        }
        .leftMap(e => List(Error(s"building a Record, got error for Schema $e")))
  }

  private def error(schemaType: String, field: Schema.Field) =
    List(Error(s"building $schemaType, expected a Schema got a Schema.Field ${field.schema()}"))

  private def fieldWithDefault[A](name: String,
                                  doc: Option[String],
                                  default: A,
                                  schema: Schema): Results[Schema.Field] =
    (schema.getType match {
      case UNION => moveDefaultToHead(schema, default)
      case _     => schema.asRight
    }).flatMap { s =>
      schemaField(name, s, doc, transformDefault(default, schema))
    }

}
