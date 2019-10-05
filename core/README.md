# Decoder

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
        }.flatMap(record => Decoder.decode[ReaderStringRecord](readerSchema, record))
```

Will, if successful, decode the relevent fields to the GenericRecord to your caseclass, using the provided reader schema

```scala
Right(ReaderStringRecord("cupcat"))
```
