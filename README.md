[![Build Status](https://travis-ci.com/bobbyrauchenberg/avronaut.svg?branch=master)](https://travis-ci.com/bobbyrauchenberg/avronaut)
[![codecov](https://codecov.io/gh/bobbyrauchenberg/avronaut/branch/master/graph/badge.svg)](https://codecov.io/gh/bobbyrauchenberg/avronaut)

# Avronaut

Scala Avro library

Currently in active development

## Schema Generation

Auto-generate Avro schemas from case classes

```scala

type CP                 = String :+: Boolean :+: Int :+: CNil

case class CoproductUnion(cupcat: CP)

AvroSchema[CoproductUnion].schema
```

Will, if successful give a Right containing an Avro schema

```
Right({
	"type": "record",
	"name": "CoproductUnion",
	"namespace": "unit.schema.CoproductUnionSpec",
	"doc": "",
	"fields": [{
		"name": "cupcat",
		"type": ["int", "boolean", "string"]
	}]
})
```

## Decoder

Decode an Avro GenericRecord to a case class

```scala
case class RecordWithMultipleFields(field1: Boolean, field2: String, field3: Int)

case class ReaderStringRecord(field2: String)

AvroSchema[RecordWithMultipleFields].schema.map { writerSchema =>
          val genericRecord = new GenericData.Record(writerSchema)
          genericRecord.put(0, true)
          genericRecord.put(1, "cupcat")
          genericRecord.put(2, 45)
          genericRecord
        }.flatMap(record => Decoder.decode[ReaderStringRecord](record))
```

Will, if successful, decode the relevent fields of the GenericRecord to your caseclass, using the provided reader schema

```scala
Right(ReaderStringRecord("cupcat"))
```

## Encoder

Encode a case class to an Avro GenericRecord

```scala
case class RecordWithUnion(field: Option[String])

val record = RecordWithUnion("cupcat".some)

Encoder.encode[RecordWithUnion](record)
```

Will, if successful, encode the case class to a Right of GenericRecord

```scala
Right({"field":"cupcat"})
```



