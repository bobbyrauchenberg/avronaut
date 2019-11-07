package unit.domain

import com.sksamuel.avro4s.AvroSortPriority

sealed trait TitleType extends Product with Serializable

object TitleType {
  @AvroSortPriority(0) case object Movie     extends TitleType
  @AvroSortPriority(1) case object Programme extends TitleType
}
