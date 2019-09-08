[![Build Status](https://travis-ci.com/bobbyrauchenberg/cupcat-avro.svg?branch=master)](https://travis-ci.com/bobbyrauchenberg/cupcat-avro)

# cupcat-avro

Scala Avro library inspired by Avro4s

Currently just builds schemas, and does case class decoding - not all cases will be covered right now

Currently Supports: 
Scala primitives which map to Avro primitives
Scala immutable collections which map to Avro Arrays
Scala immutable maps which map to Avro Maps
Avro Union types expressed using Option / Either / Shapeless Coproducts / Sealed trait hierachies of case class / case objects
Avro Enum types expressed using sealed trait hierachies of only case objects

Schema creation has support for overriding name / namespace and adding doc

Decoding currently hasn't implemented this functionality, but it will happen soon

Currently has no support for UUID / DateTime / Aliases, Fixed types and field mapping

