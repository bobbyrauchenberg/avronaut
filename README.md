[![Build Status](https://travis-ci.com/bobbyrauchenberg/cupcat-avro.svg?branch=master)](https://travis-ci.com/bobbyrauchenberg/cupcat-avro)
[![codecov](https://codecov.io/gh/bobbyrauchenberg/cupcat-avro/branch/master/graph/badge.svg)](https://codecov.io/gh/bobbyrauchenberg/cupcat-avro)

# Avronaut

Scala Avro library inspired by Avro4s

Currently just builds schemas, and does case class decoding, and very basic encoding (more coming soon)

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

Schema functionality is described in [schema](schema/README.md)

## Decoder 

Decoder functionality is described in [decoder](decoder/README.md)


## Encoder
This is coming soon

Currently there is no support in any module for UUID / DateTime / Aliases, Fixed types and field mapping

