package unit.domain

import com.sksamuel.avro4s.AvroSortPriority

sealed trait ContentType extends Product with Serializable

object ContentType {
  @AvroSortPriority(0) case object Unknown extends ContentType
  @AvroSortPriority(1) case object Title   extends ContentType
  @AvroSortPriority(2) case object Trailer extends ContentType
}
