//package com.rauchenberg.avronaut.encoder
//
//import collection.JavaConverters._
//import com.rauchenberg.avronaut.common.{AvroBoolean, AvroRecord, AvroString, AvroType}
//import org.apache.avro.Schema
//import org.apache.avro.generic.GenericData
//
//object ASTParser {
//
//  def parseAvroType(avroType: AvroType, schema: Schema, genRec: GenericData.Record): GenericData.Record = {
//
//    val genRec = new GenericData.Record(schema)
//
//    avroType match {
//
//      case AvroRecord(list) => {
//        list.zip(schema.getFields.asScala.toList).map { case (avroType, field) =>
//          parseAvroType(avroType, field.schema, new GenericData.Record(field.schema))
//        }.zipWithIndex.foreach { case (gr, ind) => genRec.put(ind, gr) }
//        genRec
//      }
//      case AvroString(value) => value
//      case AvroBoolean(value) =>  value
//
//
//    }
//  }
//
//}
