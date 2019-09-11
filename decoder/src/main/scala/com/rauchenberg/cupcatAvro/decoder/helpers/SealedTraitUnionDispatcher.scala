package com.rauchenberg.cupcatAvro.decoder.helpers

import cats.implicits._
import com.rauchenberg.cupcatAvro.common.{AvroError, Error, Result, safe}
import com.rauchenberg.cupcatAvro.decoder.Decoder.Typeclass
import com.rauchenberg.cupcatAvro.decoder.helpers.ReflectionHelpers.toCaseObject
import magnolia.SealedTrait
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericRecord}

import scala.collection.JavaConverters._

sealed trait SealedTraitUnion

case class UnionRecord[T](genericRecord: GenericData.Record, fieldName: String, schema: Schema, ctx: SealedTrait[Typeclass, T]) extends SealedTraitUnion

case class UnionEnum[T](fieldName: String, record: GenericRecord, ctx: SealedTrait[Typeclass, T]) extends SealedTraitUnion


object SealedTraitUnionDispatcher {

  val unionErrorMsg = "Was not able to create a sealed trait UNION"
  val enumErrorMsg = "couldn't instantiate ENUM for field"

  def dispatchUnionRecord[T](union: UnionRecord[T]): Either[Error, T] = {
    val recordSchema = union.genericRecord.getSchema
    val schemaFullName = recordSchema.getFullName

    val matchFullName : Schema => Boolean = _.getFullName == schemaFullName

    val findSubSchema = union.schema.getTypes.asScala.find(matchFullName).toRight(
      Error(s"$unionErrorMsg, couldn't find a matching subschema")
    )

    def createRecordToDecode(subSchema: Schema): Either[Error, GenericData.Record] = {
      val gr = new GenericData.Record(subSchema)
      recordSchema.getFields.asScala.toList
        .traverse(f => safe(gr.put(f.name, union.genericRecord.get(f.name)))).map(_ => gr)
    }

    for {
      subSchema <- findSubSchema
      subType <- findSubtypeToDispatch(schemaFullName, union.ctx, unionErrorMsg)
      subRecord <- createRecordToDecode(subSchema)
      decodedT <- subType.typeclass.decodeFrom(union.fieldName, subRecord).map(_.asInstanceOf[T])
    } yield decodedT
  }

  def dispatchUnionEnum[T](union: UnionEnum[T]) =
    safe(
      findSubtypeToDispatch(union.record.get(union.fieldName).asInstanceOf[Schema].getFullName, union.ctx, enumErrorMsg)
        .map(st => toCaseObject[T](st.typeName.full))
        .leftMap(_ => Error(s"$enumErrorMsg ${union.fieldName}"))
    ).flatten

  private def findSubtypeToDispatch[T](typeToMatch: String, ctx: SealedTrait[Typeclass, T], errorPrefix: String) =
    ctx.subtypes.find(_.typeName.full == typeToMatch).toRight(
      Error(s"$errorPrefix, couldn't find subtype matching $typeToMatch")
    )


}
