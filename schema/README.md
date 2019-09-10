# Schema

Create an Avro schema for a record 

```scala
case class MyRecordType(name: String, age: Int)

AvroSchema[MyRecordType].schema
```

Will produce an Either contanining an Avro Schema instance

```scala
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

For this to compile there needs to be an implicit instance of `com.rauchenberg.cupcatAvro.schema.AvroSchema` in scope for each type contained in the case class. 
