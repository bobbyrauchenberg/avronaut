package unit.domain

import com.sksamuel.avro4s.AvroSortPriority

sealed trait Role extends Product with Serializable

@AvroSortPriority(0) case object Unknown                                                  extends Role
@AvroSortPriority(1) final case class Actor(characterId: Option[String], isStar: Boolean) extends Role
@AvroSortPriority(2) case object Director                                                 extends Role
@AvroSortPriority(3) case object ExecutiveProducer                                        extends Role
@AvroSortPriority(4) case object Producer                                                 extends Role
@AvroSortPriority(5) case object Writer                                                   extends Role
@AvroSortPriority(6) final case class Other(role: String)                                 extends Role
