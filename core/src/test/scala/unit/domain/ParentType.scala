package unit.domain

import com.sksamuel.avro4s.AvroSortPriority

sealed trait ParentType extends Product with Serializable

object ParentType {
  @AvroSortPriority(0) case object Season extends ParentType
  @AvroSortPriority(1) case object Series extends ParentType
}
