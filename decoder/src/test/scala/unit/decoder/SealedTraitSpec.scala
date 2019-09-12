package unit.decoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.cupcatAvro.decoder.DecodeTo
import com.rauchenberg.cupcatAvro.schema.AvroSchema
import org.apache.avro.generic.GenericData
import unit.common.UnitSpecBase
import SealedTraitInstances._

class SealedTraitSpec extends UnitSpecBase {

  "decoder" should {

    "handle sealed trait enums" in {
      forAll { enum: SealedTraitEnum =>
        val schema = AvroSchema[SealedTraitEnum].schema

        val record = new GenericData.Record(schema.value)

        record.put("field", enum.field.toString)
        DecodeTo[SealedTraitEnum](record) should beRight(enum)
      }
    }

    "handle sealed trait enums with defaults" in {
      val schema = AvroSchema[SealedTraitEnumWithDefault].schema

      val record = new GenericData.Record(schema.value)

      DecodeTo[SealedTraitEnumWithDefault](record) should beRight(SealedTraitEnumWithDefault())
    }

    "handle sealed trait records" in {
      val record = new GenericData.Record(AvroSchema[SealedTraitUnion].schema.value)

      val cat    = new GenericData.Record(AvroSchema[Cat].schema.value)
      val rendal = new GenericData.Record(AvroSchema[Rendal].schema.value)

      val cup = AvroSchema[Cup.type].schema.value
      cat.put("field", "cupcat")
      cat.put("name", "cuppers")
      rendal.put(0, "rendal")
      record.put("field", cat)
      record.put("strField", "snoutley")
      record.put("field2", rendal)
      record.put("field3", cup)

      DecodeTo[SealedTraitUnion](record) should beRight(
        SealedTraitUnion(Cat("cupcat", "cuppers"), "snoutley", Rendal("rendal"), Cup))
    }

  }

}

private[this] object SealedTraitInstances {
  sealed trait CupcatEnum
  case object Cuppers  extends CupcatEnum
  case object Snoutley extends CupcatEnum

  sealed trait CupcatUnion
  case object Cup                             extends CupcatUnion
  case class Cat(field: String, name: String) extends CupcatUnion
  case class Rendal(name: String)             extends CupcatUnion

  case class SealedTraitEnum(field: CupcatEnum)
  case class SealedTraitUnion(field: CupcatUnion, strField: String, field2: CupcatUnion, field3: CupcatUnion)

  case class SealedTraitEnumWithDefault(field: CupcatEnum = Cuppers)
}
