[![Build Status](https://travis-ci.com/bobbyrauchenberg/avronaut.svg?branch=master)](https://travis-ci.com/bobbyrauchenberg/avronaut)
[![codecov](https://codecov.io/gh/bobbyrauchenberg/avronaut/branch/master/graph/badge.svg)](https://codecov.io/gh/bobbyrauchenberg/avronaut)

# Avronaut

Scala Avro library, currently in active development

Inspired by Avro4S, Avronaut is aiming to offer
 - Safety. It doesn't throw exceptions, and offers well typed client APIs
 - Simplicity. Encoding an decoding require only the use of a single `Codec` API
 - Speed. The aim is to make it as fast, or in some cases faster, than Avro4S

It also offers an error accumulation mode to help debugging issues with encoding and decoding

## Encoding and Decoding

Avronaut provides a simple `Codec` API which allows you to encode case classes to Avro, decode Avro `GenericRecord` instances, and generate avro schema objects. 

```scala
import Codec._

case class Record(field1: String, field2: Int)

implicit val codec = Codec[Record]

val record = Record("cupcat", 123)

val asGenericRecord = record.encode // Right({"value": "cupcat", "value2": 123})

asGenericRecord.flatMap { genericRecord => genericRecord.decode[Record] } // Right(Record(cupcat,123)) 

Codec.schema[Record] // Right({"type":"record","name":"Record","namespace": ...
```

### Encoder

The `Codec` API is probably fine for most use cases, but in some cases you may only need to encode your data (if your app only writes and does not read from Kafka for example), in this case you can choose to use the `Encoder` API. 

```scala
case class RecordWithUnion(field: Option[String])

val encoder = Encoder.toEncoder[RecordWithUnion]
val toEncode = RecordWithUnion("cupcat".some)

Encoder.encode[RecordWithUnion](toEncode, encoder) 
```

### Decoder

If your app only needs to decode data you can use the `Decoder` API, though this API doesn't generate a standalone schema, so if you also need an Avro schema object for some reason, you would be better off using `Codec`

```scala
case class RecordWithMultipleFields(field1: Boolean, field2: String, field3: Int)

val decoder = Decoder[RecordWithMultipleFields]

Decoder.decode[RecordWithMultipleFields](someGenericRecord, decoder) 
```

### Error Accumulation

To help debug decode and encode failures Avronuat offers apis which accumulate errors, in the style of Validation

```scala
case class RecordWithMultipleFields(field1: Boolean, field2: String, field3: Int)

val decoder = Decoder[RecordWithMultipleFields]

Decoder.decodeAccumulating[RecordWithMultipleFields](someGenericRecord, decoder) 
```

If there are any errors Avronaut will return a List of the fields which failed. In this case if `field1` and `field3` fail that will be described, along with a printout of the provided GenericRecord

```scala
Left(
  List(
    Error("Decoding failed for param 'field1' with value '123' from the GenericRecord"),
    Error("Decoding failed for param 'field3' with value 'cupcat' from the GenericRecord"),
    Error("The failing GenericRecord was '{"field1" : "123", "field2" : "cupcat", "field3" : "cupcat"})
  )
)  
```

## Schemas

Encoding and decoding using Avronaut does not require you to pass schemas around either explicitly or implicitly. However sometimes you might need access to the schema that has been generated. It can be retrieved via the Encoder API

```scala
case class Record(field1: Int, field2: String, field3: Boolean)

val encoder = Encoder.toEncoder[Record]

Encoder.schemaFrom(encoder)
```

This will return the `AvroSchema` object generated from the supplied case class




Similarly `Encoder` offers the `encodeAccumulating` function which will return a list describing fields which failed


