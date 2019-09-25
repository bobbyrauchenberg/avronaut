package com.rauchenberg.avronaut.decoder

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

import cats.implicits._
import com.rauchenberg.avronaut.common.AvroType._
import com.rauchenberg.avronaut.common.{
  safe,
  AvroArray,
  AvroBoolean,
  AvroBytes,
  AvroDouble,
  AvroEnum,
  AvroFail,
  AvroField,
  AvroFloat,
  AvroInt,
  AvroLong,
  AvroMap,
  AvroMapEntry,
  AvroNull,
  AvroRecord,
  AvroString,
  AvroTimestampMillis,
  AvroType,
  AvroUUID,
  AvroUnion,
  Error,
  Result
}
import com.rauchenberg.avronaut.decoder.helpers.ReflectionHelpers
import magnolia.{CaseClass, Magnolia, SealedTrait}
import org.apache.avro.Schema.Type._
import org.apache.avro.generic.GenericRecord
import org.apache.avro.{LogicalType, Schema}
import shapeless.{:+:, CNil, Coproduct, Inr}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.convert.Wrappers.{MapWrapper, SeqWrapper}

sealed trait DecodeOperation
case class FullDecode(schema: Schema, genericRecord: GenericRecord) extends DecodeOperation
case class FieldDecode(value: AvroType)                             extends DecodeOperation

trait Decoder[A] {

  def apply(operation: DecodeOperation): Result[A]

}

object Decoder {

  def apply[A](implicit decoder: Decoder[A]) = decoder

  type Typeclass[A] = Decoder[A]

  implicit def gen[A]: Typeclass[A] = macro Magnolia.gen[A]

  def combine[A](ctx: CaseClass[Typeclass, A]): Typeclass[A] = new Typeclass[A] {
    override def apply(cursor: DecodeOperation): Result[A] = {

      val toParse: Result[AvroRecord] = cursor match {
        case FullDecode(schema, record)     => parse(FullDecode(schema, record)).map(v => AvroRecord(v.toList))
        case FieldDecode(a @ AvroRecord(_)) => a.asRight
        case _                              => Error("expected a top cursor or an AvroRecord").asLeft
      }

      val params = ctx.parameters

      toParse.flatMap { av =>
        val list = av.value
        list.zip(params).traverse {
          case (at, param) =>
            val toDispatch = at match {
              case AvroField(v) => v
              case other        => other
            }
            val tcRes = param.typeclass(FieldDecode(toDispatch))
            (tcRes, param.default) match {
              case (Left(_), Some(default)) => default.asRight
              case _                        => tcRes
            }
        }
      }.map(ctx.rawConstruct(_))

    }

    def parse(cursor: DecodeOperation) = {
      val (schema, genRec) = cursor match {
        case FullDecode(schema, record) => (schema, record)
        case _                          => throw new RuntimeException("fix later")
      }

      fieldsFor(schema).traverse { field =>
        val value       = genRec.get(field.name)
        val errorValues = s"'$value', '${schema}'"
        val schemaType  = field.schema.getType

        schemaType match {
          case RECORD =>
            parseRecord(field.schema, value)
          case UNION =>
            parseUnion(field.schema, value).map(AvroUnion(_))
          case ARRAY =>
            parseArray(field.schema, value)
              .fold(_ => AvroFail(s"couldn't parse array $errorValues").asRight, _.asRight)
          case ENUM =>
            AvroEnum(value).asRight
          case MAP =>
            parseMap(field.schema, value)
          case _ =>
            toAST(field.schema.getLogicalType, schemaType, value, AvroField(_))
        }
      }
    }

    def parseMap[A](schema: Schema, value: A): Result[AvroType] = {

      val schemaType = schema.getValueType.getType
      val valueType  = schema.getValueType

      safe(
        value
          .asInstanceOf[MapWrapper[String, A]]
          .asScala
          .toMap[String, A]
          .toList
          .traverse {
            case (k, v) =>
              (schemaType match {
                case RECORD => parseRecord(valueType, v)
                case ARRAY  => parseArray(valueType, v)
                case UNION  => parseUnion(valueType, v)
                case _      => toAST(schema.getLogicalType, schemaType, v)
              }).map(AvroMapEntry(k, _))
          }
          .map(AvroMap(_))).flatten

    }

    private def parseArray[A](schema: Schema, value: A): Result[AvroType] = {

      val failureMsg = s"parseArray can't cast '$value' to SeqWrapper for '$schema'"

      val elementType = schema.getElementType

      safe(value.asInstanceOf[SeqWrapper[A]].asScala.toVector).fold(
        _ => Error(failureMsg).asLeft,
        _.traverse { value =>
          schema.getElementType.getType match {
            case RECORD =>
              parseRecord(elementType, value)
            case UNION =>
              parseUnion(elementType, value).flatMap(toAvroUnion(_))
            case MAP =>
              parseMap(elementType, value)
            case ENUM =>
              toAvroEnum(value)
            case _ =>
              toAST(schema.getLogicalType, elementType.getType, value)
          }
        }.flatMap(toAvroArray(_))
      )

    }

    private def parseUnion[A](schema: Schema, value: A): Result[AvroType] = {

      val failureMsg = s"couldn't parse union for '$value', '$schema'"

      @tailrec
      def loop(schemas: List[Schema]): Result[AvroType] = schemas match {

        case Nil => AvroFail(failureMsg).asRight
        case h :: t =>
          val schemaType = h.getType

          (schemaType match {
            case NULL => toAvroNull(value)
            case ARRAY =>
              parseArray(h, value)
            case ENUM =>
              toAvroEnum(value)
            case RECORD =>
              typesFor(schema)
                .filter(_.getType == RECORD)
                .foldLeft(Error("wasn't able to parse a union").asLeft[AvroType]) {
                  case (acc, recordSchema) =>
                    if (acc.isRight) acc
                    else
                      parseRecord(recordSchema, value) match {
                        case r @ Right(AvroRecord(_)) => r
                        case _                        => acc
                      }
                }
            case _ =>
              toAST(schema.getLogicalType, schemaType, value)
          }) match {
            case Right(AvroFail(_)) => loop(t)
            case Right(v)           => v.asRight
            case _                  => loop(t)
          }
      }

      loop(enumToEndOfUnion(schema))
    }

    private def enumToEndOfUnion(schema: Schema) = {
      val schemaTypes = typeListFor(schema)
      schemaTypes.find(_.getType == ENUM).fold(schemaTypes) { enumSchema =>
        schemaTypes.filterNot(_.getType == ENUM) :+ enumSchema
      }
    }

    private def parseRecord[A](schema: Schema, value: A): Result[AvroType] = {
      val failureMsg = s"expected a GenericRecord for '$value', '$schema'"
      value match {
        case gr: GenericRecord => parse(FullDecode(schema, gr)).map(v => AvroRecord(v.toList))
        case _                 => Error(failureMsg).asLeft
      }
    }

    private def toAST[A](logicalType: LogicalType,
                         schemaType: Schema.Type,
                         value: A,
                         transform: AvroType => AvroType = identity) =
      Option(logicalType).fold[Result[AvroType]](avroToAST(schemaType, value).map(transform)) { lt =>
        logicalAvroToAST(lt.getName, value)
      }

    private def avroToAST[A](field: Schema.Type, value: A) = field match {
      case STRING  => toAvroString(value)
      case INT     => toAvroInt(value)
      case LONG    => toAvroLong(value)
      case FLOAT   => toAvroFloat(value)
      case DOUBLE  => toAvroDouble(value)
      case BOOLEAN => toAvroBool(value)
      case BYTES   => toAvroBytes(value)
      case NULL    => toAvroNull(value)
      case _       => Error("boom").asLeft
    }

    private def logicalAvroToAST[A](logicalTypeName: String, value: A): Result[AvroType] =
      logicalTypeName match {
        case "uuid" =>
          toAvroUUID(value)
        case "timestamp-millis" => toAvroTimestamp(value)
        case _                  => Error(s"logical type $logicalTypeName currently not supported").asLeft
      }

    private def typesFor(s: Schema) = s.getTypes.asScala.toVector

    private def typeListFor(s: Schema) = s.getTypes.asScala.toList

    private def fieldsFor(s: Schema) = s.getFields.asScala.toVector

  }

  def dispatch[A](ctx: SealedTrait[Typeclass, A]): Typeclass[A] = new Typeclass[A] {
    override def apply(value: DecodeOperation): Result[A] =
      value match {
        case FieldDecode(AvroEnum(v)) =>
          ctx.subtypes
            .find(_.typeName.short == v.toString)
            .map(st => safe(ReflectionHelpers.toCaseObject[A](st.typeName.full)))
            .getOrElse(Error(s"wasn't able to find or to instantiate enum value $v in $v").asLeft[A])
        case _ => Error("not an enum").asLeft
      }
  }

  implicit val stringDecoder = new Decoder[String] {
    override def apply(value: DecodeOperation): Result[String] =
      value match {
        case FieldDecode(AvroString(v)) => v.asRight
        case other                      => Error(s"expected a string, got $other").asLeft
      }
  }

  implicit val booleanDecoder = new Decoder[Boolean] {
    override def apply(value: DecodeOperation): Result[Boolean] =
      value match {
        case FieldDecode(AvroBoolean(v)) => v.asRight
        case _                           => Error("expected a bool").asLeft
      }

  }

  implicit val intDecoder = new Decoder[Int] {
    override def apply(value: DecodeOperation): Result[Int] =
      value match {
        case FieldDecode(AvroInt(v)) => v.asRight
        case _                       => Error(s"int decoder expected an int, got $value").asLeft
      }
  }

  implicit val longDecoder = new Decoder[Long] {
    override def apply(value: DecodeOperation): Result[Long] =
      value match {
        case FieldDecode(AvroLong(v)) => v.asRight
        case _                        => Error("expected a long").asLeft
      }
  }

  implicit val floatDecoder = new Decoder[Float] {
    override def apply(value: DecodeOperation): Result[Float] =
      value match {
        case FieldDecode(AvroFloat(v)) => v.asRight
        case _                         => Error("expected a float").asLeft
      }
  }

  implicit val doubleDecoder = new Decoder[Double] {
    override def apply(value: DecodeOperation): Result[Double] =
      value match {
        case FieldDecode(AvroDouble(v)) => v.asRight
        case _                          => Error(s"double decoder expected a double, got $value").asLeft
      }
  }

  implicit val bytesDecoder = new Decoder[Array[Byte]] {
    override def apply(value: DecodeOperation): Result[Array[Byte]] =
      value match {
        case FieldDecode(AvroBytes(v)) => v.asRight
        case _                         => Error("bytes decoder expected an Array[Byte]").asLeft
      }
  }

  implicit def listDecoder[A](implicit elementDecoder: Decoder[A]) = new Decoder[List[A]] {
    override def apply(value: DecodeOperation): Result[List[A]] = value match {
      case FieldDecode(AvroArray(v)) =>
        v.traverse(at => elementDecoder.apply(FieldDecode(at)))
      case other => Error(s"list decoder expected to get a list, got $other").asLeft
    }
  }

  implicit def seqDecoder[A](implicit elementDecoder: Decoder[A]) = new Decoder[Seq[A]] {
    override def apply(value: DecodeOperation): Result[Seq[A]] = listDecoder[A].apply(value)
  }

  implicit def vectorDecoder[A](implicit elementDecoder: Decoder[A]) = new Decoder[Vector[A]] {
    override def apply(value: DecodeOperation): Result[Vector[A]] = listDecoder[A].apply(value).map(_.toVector)
  }

  implicit def mapDecoder[A](implicit elementDecoder: Decoder[A]) = new Typeclass[Map[String, A]] {

    override def apply(value: DecodeOperation): Result[Map[String, A]] = value match {
      case FieldDecode(AvroMap(l)) =>
        l.traverse { entry =>
          elementDecoder(FieldDecode(entry.value)).map(entry.key -> _)
        }.map(_.toMap[String, A])
      case _ => Error(s"expected an AvroMap, got $value").asLeft
    }
  }

  implicit def offsetDateTimeDecoder = new Decoder[OffsetDateTime] {
    override def apply(value: DecodeOperation): Result[OffsetDateTime] = value match {
      case FieldDecode(AvroTimestampMillis(AvroLong(value))) =>
        safe(OffsetDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC))
      case _ => Error(s"OffsetDateTime decoder expected an AvroLong, got $value").asLeft
    }
  }

  implicit def instantDecoder = new Decoder[Instant] {
    override def apply(value: DecodeOperation): Result[Instant] = value match {
      case FieldDecode(AvroTimestampMillis(AvroLong(value))) => safe(Instant.ofEpochMilli(value))
      case _                                                 => Error(s"OffsetDateTime decoder expected an AvroLong, got $value").asLeft
    }
  }

  implicit def uuidDecoder = new Decoder[UUID] {
    override def apply(value: DecodeOperation): Result[UUID] = value match {
      case FieldDecode(AvroUUID(AvroString(value))) => safe(java.util.UUID.fromString(value))
      case _                                        => Error(s"UUID decoder expected an AvroUUID, got $value").asLeft
    }
  }

  implicit def optionDecoder[A](implicit valueDecoder: Decoder[A]) = new Decoder[Option[A]] {
    override def apply(value: DecodeOperation): Result[Option[A]] =
      value match {
        case FieldDecode(AvroUnion(AvroNull)) => None.asRight
        case FieldDecode(AvroUnion(value))    => valueDecoder(FieldDecode(value)).map(Option(_))
        case other                            => valueDecoder(other).map(Option(_))
      }
  }

  implicit def eitherDecoder[A, B](implicit lDecoder: Decoder[A], rDecoder: Decoder[B]) = new Decoder[Either[A, B]] {
    override def apply(value: DecodeOperation): Result[Either[A, B]] = {
      def runDecoders(value: DecodeOperation) =
        lDecoder(value).fold(_ => rDecoder(value).map(_.asRight), _.asLeft.asRight)
      value match {
        case FieldDecode(AvroUnion(value)) if (!value.isNull) => runDecoders(FieldDecode(value))
        case other                                            => runDecoders(other)
      }
    }
  }

  implicit object CNilDecoderValue extends Decoder[CNil] {
    override def apply(value: DecodeOperation): Result[CNil] = Error("decoded CNil").asLeft
  }

  implicit def coproductDecoder[H, T <: Coproduct](implicit hDecoder: Decoder[H], tDecoder: Decoder[T]) =
    new Decoder[H :+: T] {
      override def apply(value: DecodeOperation): Result[H :+: T] = value match {
        case FieldDecode(AvroUnion(v)) =>
          hDecoder(FieldDecode(v)) match {
            case r @ Right(_) => r.map(h => Coproduct[H :+: T](h))
            case _            => tDecoder(value).map(Inr(_))
          }
        case _ => tDecoder(value).map(Inr(_))
      }
    }

}
