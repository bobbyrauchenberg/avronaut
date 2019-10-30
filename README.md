[![Build Status](https://travis-ci.com/bobbyrauchenberg/avronaut.svg?branch=master)](https://travis-ci.com/bobbyrauchenberg/avronaut)
[![codecov](https://codecov.io/gh/bobbyrauchenberg/avronaut/branch/master/graph/badge.svg)](https://codecov.io/gh/bobbyrauchenberg/avronaut)

# Avronaut

Scala Avro library

Currently in active development

Inspired by Avro4S, Avronaut is aiming to offer safety (no exceptions, strongly typed client APIS), whilst being simple to use and fast for encoding and decoding. 

It also offers an error accumulation mode to help debugging issues with encoding and decoding

## Schema Generation

Auto-generate Avro schemas from case classes

```scala
case class RecordWithList(field: List[String])
val schema: AvroSchema[RecordWithList] = AvroSchema[RecordWithList] = AvroSchema.toSchema[RecordWithList]
```

## Decoder

Decode an Avro GenericRecord to a case class

```scala
case class RecordWithMultipleFields(field1: Boolean, field2: String, field3: Int)

val decoder = Decoder[RecordWithMultipleFields]
val genericRecord = ...

Decoder.decode[RecordWithMultipleFields](genericRecord, decoder) //Right(RecordWithMultipleFields(true, "some string", 123))
```

Decode an Avro GenericRecord to a case class accumulating all failures

```scala
case class RecordWithMultipleFields(field1: Boolean, field2: String, field3: Int)

val decoder = Decoder[RecordWithMultipleFields]
val genericRecord = ...

Decoder.decodeAccumulating[RecordWithMultipleFields](genericRecord, decoder) 
```

If there are any errors Avronaut will return a List describing the fields which failed. In this case if `field1` and `field3` fail that will be described, along with a printout of the GenericRecord itself

```scala
Left(
  List(
    Error("Decoding failed for param 'field1' with value '123' from the GenericRecord"),
    Error("Decoding failed for param 'field3' with value 'cupcat' from the GenericRecord"),
    Error("The failing GenericRecord was '{"field1" : "123", "field2" : "cupcat", "field3" : "cupcat" }))
```

## Encoder

Encode a case class to an Avro GenericRecord

Encoding needs a schema to generate a GenericRecord from

```scala
case class RecordWithUnion(field: Option[String])

val schema = AvroSchema.toSchema[RecordWithUnion]
val encoder = Encoder[RecordWithUnion]
val record = RecordWithUnion("cupcat".some)

Encoder.encode[RecordWithUnion](record, encoder, schema.data) // Right({"field":"cupcat"})
```

As with the decoder, you can encode a case class to an Avro GenericRecord accumulating all failures

```scala
Encoder.encodeAccumulating[RecordWithUnion](record, encoder, schema.data) 
```

Again, this will return a list describing the fields which failed


