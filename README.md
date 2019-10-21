[![Build Status](https://travis-ci.com/bobbyrauchenberg/avronaut.svg?branch=master)](https://travis-ci.com/bobbyrauchenberg/avronaut)
[![codecov](https://codecov.io/gh/bobbyrauchenberg/avronaut/branch/master/graph/badge.svg)](https://codecov.io/gh/bobbyrauchenberg/avronaut)

# Avronaut

Scala Avro library, currently in active development

Inspired by Avro4S, Avronaut is aiming to offer
 - Safety. It doesn't throw exceptions, and offers well typed client APIs
 - Simplicity. Encoding and decoding require only the use of a single `Codec` API
 - Speed. The aim is to make it as fast, or in some cases faster, than Avro4S

It also offers an error accumulation mode to help debugging issues with encoding and decoding

Contents
========

   * [Avronaut](#avronaut)
      * [Quick Start](#quick-start)
         * [Encoding](#encoding)
         * [Decoding](#decoding)
         * [Schemas](#schemas)
         * [Error Accumulation](#error-accumulation)
         * [Encoder and Decoder APIs](#encoder-and-decoder-apis)
            * [Encoder](#encoder)
            * [Decoder](#decoder)
      * [Adding typeclass instances](#adding-typeclass-instances)
         * [Codec instances](#codec-instances)
         * [Encoder instances](#encoder-instances)
         * [Decoder instances](#decoder-instances)
         * [SchemaBuilder](#schemabuilder)
         * [EncoderBuilder](#encoderbuilder)
         * [DecoderBuilder](#decoderbuilder)

## Quick Start

Avronaut provides a simple `Codec` API which allows you to encode case classes to Avro, decode Avro `GenericRecord` instances, and generate Avro schema objects. 

### Encoding

To generate a `GenericRecord` from a case class

```scala
import Codec._

case class Record(field1: String, field2: Int)
implicit val codec = Codec[Record]

val record = Record("cupcat", 123)

val asGenericRecord = record.encode // Right({"value": "cupcat", "value2": 123})
```

### Decoding

To decode from a `GenericRecord` to a case class

```scala
import Codec._

case class Record(field1: String, field2: Int)
implicit val codec = Codec[Record]

Codec.schema[Record].map { schema =>
        val genericRecord: GenericData.Record = new GenericData.Record(schema)
        genericRecord.put(0, "cupcat")
        genericRecord.put(1, 123)
        genericRecord.decode[Record]
      }

// Right(Record(cupcat,123))
```

### Schemas

The codec API gives access to the Avro Schema object generated from your case class

```scala
import Codec._

case class Record(field1: String, field2: Int)
val codec = Codec[Record]

codec.schema // Right({{"type":"record","name":"Record","namespace":"com.rauchenberg ..."})
```

Alternately, if your codec instance is available in implicit scope you can do the following

```scala
import Codec._

case class Record(field1: String, field2: Int)

Codec.schema[Record] // Right({{"type":"record","name":"Record","namespace":"com.rauchenberg ..."})
```

### Error Accumulation

To help debug decode and encode failures Avronuat offers apis which accumulate errors, in the style of Validation. These are named `encodeAccumulating` and `decodeAccumulating` respectively
```scala
import Codec._

case class Record(field1: String, field2: Int)

implicit val codec = Codec[Record]

val genericRecord = ...

genericRecord.decodeAccumulating[Record]
```

If there are any errors Avronaut will return a List of the fields which failed. In this case if `field1` and `field2` failed that will be described, along with a printout of the provided GenericRecord

```scala
Left(
  List(
    Error("Decoding failed for param 'field1' with value 'cupcat' from the GenericRecord"),
    Error("Decoding failed for param 'field2' with value '123' from the GenericRecord"),
    Error("The failing GenericRecord was '{"field1" : "cupcat", "field2" : 123})
  )
)  
```

### Encoder and Decoder APIs

If your application only needs to either encode or decode data, there are also `Encoder` and `Decoder` apis. These work in the same way as the `Codec` api. 

#### Encoder
```scala
import Encoder._

case class Record(field1: String, field2: Int)
implicit val encoder = Encoder[Record]

val record = Record("cupcat", 123)
record.encode
```

#### Decoder
```scala
import Codec._

case class Record(field1: String, field2: Int)
implicit val decoder = Decoder[Record]

val genericRecord = ???
genericRecord.decode[Record]
```





## Adding typeclass instances

Avronaut uses Magnolia for automatic typeclass derivation. If you wish to encode from, or decode to a caseclass you need to have the relevant instances in scope for each field in that case class. 

### Codec instances

If you are using the `Codec` API you need ensure there are typeclass instances provided for
1. Schema creation - requires an instance of `SchemaBuilder`
2. Encoding - requires an instance of `EncoderBuilder`
3. Decoding - requires an instance of `DecoderBuilder`

### Encoder instances

If you are using the `Encoder` API you need ensure there are typeclass instances provided for
1. Schema creation - requires an instance of `SchemaBuilder`
2. Encoding - requires an instance of `EncoderBuilder`

### Decoder instances
If you are using the `Decoder` API you only need instances for `DecoderBuilder`


### SchemaBuilder

The `SchemaBuilder` API is used to define typeclass instances used for converting caseclasses into AvroSchema objects. `SchemaBuilder` uses an ADT to represent Avro types as defined in the Avro specification [https://avro.apache.org/docs/1.9.1/spec.html#schemas]

Adding a new typeclass instance for a custom type involves picking the relevant ADT type as defined in `AvroSchemaADT` and lifting it into `Either`.

For example, this is how the instance for `OffsetDateTime` is defined

```scala
  implicit def offsetDateTimeSchema[A] = new SchemaBuilder[OffsetDateTime] {
    override def schema: Results[AvroSchemaADT] = SchemaTimestampMillis.asRight
  }
```

### EncoderBuilder

The `EncoderBuilder` API is used to define how caseclass fields are translated into a format which can be stored in a `GenericRecord` 

This is how the instance for `Int` is defined

```scala
   implicit val intEncoder: EncoderBuilder[Int] = new EncoderBuilder[Int] {
    type Ret = Int
    override def apply(value: Int, schemaData: SchemaData, failFast: Boolean): Int = java.lang.Integer.valueOf(value)
  }
```

This instance gets the `Int` value in the caseclass that is being derived, and converts it to a java `Integer`. If you are in doubt about the value of the abstract `Ret` field, `Any` will work fine. `Ret` is mainly only there because I didn't like seeing `Any` in the API :D

Originally all `EncoderBuilder` instances returned `Either[..,..]` but after profiling I found it was faster to do it like this, as far as the end user is concerned this is safe as exceptions won't propogate up to them.


### DecoderBuilder

The `DecoderBuilder` API defines the instances used to go from values stored in a `GenericRecord` to the types that make up  the caseclass that is being derived.

The example below shows the instance for `UUID`

```scala
  implicit def uuidDecoderBuilder: DecoderBuilder[UUID] = new DecoderBuilder[UUID] {
    override def apply[B](value: B, failFast: Boolean): Results[UUID] = value match {
      case s: String if (s != null) => Right(java.util.UUID.fromString(s))
    }
  }
```

The `failFast` parameter is currently ignored inside field-specific typeclass instances - the value is used for defining whether or not to accumulate errors, but currently this is only taken into account at the record level, so you can ignore it when defining an instance. The example above shows that the `UUID` decoder expects to get a String (UUIDs are encoded as String) and then converts that to a UUID.

You can make these instances partial, in the case of a match failure exceptions will not propogate up to the users.

