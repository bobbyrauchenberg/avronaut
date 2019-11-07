package unit.domain

import com.sksamuel.avro4s.AvroSortPriority

sealed trait RegionAvailability extends Product with Serializable

@AvroSortPriority(0) case object Unavailable                     extends RegionAvailability
@AvroSortPriority(1) case object Global                          extends RegionAvailability
@AvroSortPriority(2) final case class BouquetType(value: String) extends RegionAvailability
