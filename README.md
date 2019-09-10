[![Build Status](https://travis-ci.com/bobbyrauchenberg/cupcat-avro.svg?branch=master)](https://travis-ci.com/bobbyrauchenberg/cupcat-avro)

# cupcat-avro

Scala Avro library inspired by Avro4s

Currently just builds schemas, and does case class decoding - not all cases will be covered right now

Currently Supports: 
* Scala primitives which map to Avro primitives
* Scala immutable collections which map to Avro Arrays
* Scala immutable maps which map to Avro Maps
* Avro Union types encoded using
  * Option
  * Either
  * Shapeless Coproducts 
  * Sealed trait hierachies of case class and / or case objects
* Avro Enum types encoded using sealed trait hierachies of case objects

## Schema 

Create an Avro schema for a record

```scala
case class MyRecordType(name: String, age: Int)

AvroSchema[MyRecordType].schema

Right({
	"type": "record", "name": "MyRecordType", "namespace": "my.org.com", "doc": "",
	"fields": [{
		"name": "name",
		"type": "string"
	}, {
		"name": "age",
		"type": "int"
	}]
})
```

The schema module has support for setting:
* Avro name
* Avro namespace
* Avro doc field

## Decoder 


## Encoder
This is coming soon

Currently there is no support in any module for UUID / DateTime / Aliases, Fixed types and field mapping

