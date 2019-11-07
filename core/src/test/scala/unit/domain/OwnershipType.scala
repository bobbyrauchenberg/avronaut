package unit.domain

import com.sksamuel.avro4s.AvroSortPriority

sealed trait OwnershipType

object OwnershipType {
  @AvroSortPriority(0) case object Unknown extends OwnershipType
  @AvroSortPriority(1) case object Own     extends OwnershipType
  @AvroSortPriority(2) case object Rent    extends OwnershipType
  @AvroSortPriority(3) case object Free    extends OwnershipType
}
