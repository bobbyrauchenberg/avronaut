package unit.domain

import java.time.{Duration, OffsetDateTime}
import cats.instances.all._
import cats.syntax.foldable._
import com.sksamuel.avro4s.{AvroDoc, AvroSortPriority}
import unit.domain.CommonEntityFields.{EntityId, Language}
import unit.domain.{Title => DomainTitle}

sealed trait CanonicalEntity extends Product with Serializable

object CanonicalEntity {
  implicit class CanonicalEntityOps(val entity: CanonicalEntity) extends AnyVal {
    def id: EntityId = entity match {
//      case entity: TitleEntity       => entity.id
//      case entity: SeasonEntity      => entity.id
//      case entity: SeriesEntity      => entity.id
//      case entity: SportEntity       => entity.id
//      case entity: TeamEntity        => entity.id
//      case entity: CompetitionEntity => entity.id
//      case entity: OfferEntity       => entity.id
//      case entity: ServiceEntity     => entity.id.toString
//      case entity: RegionService     => entity.id
//      case entity: RegionEntity      => entity.id
//      case entity: ContributorEntity => entity.id
      case entity: Credit =>
        val roleStr = entity.role match {
          case Actor(charId, _)  => s"actor${charId.foldMap(cid => s"-$cid")}"
          case Director          => "director"
          case Producer          => "producer"
          case ExecutiveProducer => "executiveproducer"
          case Writer            => "writer"
          case Other(role)       => role
          case Unknown           => "unknown"
        }
        s"${entity.entityId}-${entity.contributorId}-$roleStr"
      case _ => throw new RuntimeException("boom!")
    }
  }
}

object CommonEntityFields {
  type Language = String

  type EntityId = String
}

sealed trait EntityDetails extends Product with Serializable

final case class TitleEntity(id: EntityId,
                             `type`: DomainTitle.Type = Title.Unknown,
                             @AvroDoc("Deprecated in favour of type") titleType: TitleType,
                             localizedDetails: Map[Language, TitleDetails],
                             @AvroDoc("Deprecated in favour of type") parent: Option[Parent] = None,
                             @AvroDoc("Deprecated in favour of type") episodeNumber: Option[Int] = None,
                             @AvroDoc("Deprecated in favour of type") lastInSeason: Boolean,
                             forNeverMiss: Boolean,
                             genre: Option[String] = None,
                             subGenres: Set[String],
                             tags: Set[String],
                             productionYear: Option[Int] = None,
                             ageRatings: Option[AgeRatings] = None)
    extends CanonicalEntity

final case class TitleDetails(title: String,
                              shortSynopsis: Option[String] = None,
                              mediumSynopsis: Option[String] = None,
                              longSynopsis: Option[String] = None)
    extends EntityDetails

final case class Parent(parentType: ParentType, id: EntityId)

final case class SeasonEntity(id: EntityId,
                              localizedDetails: Map[Language, SeasonDetails],
                              seriesId: EntityId,
                              seasonNumber: Int,
                              genre: Option[String] = None,
                              subGenres: Set[String],
                              tags: Set[String],
                              productionYear: Option[Int] = None)
    extends CanonicalEntity

final case class SeasonDetails(title: String,
                               shortSynopsis: Option[String] = None,
                               mediumSynopsis: Option[String] = None,
                               longSynopsis: Option[String] = None)
    extends EntityDetails

final case class SeriesEntity(id: EntityId,
                              localizedDetails: Map[Language, SeriesDetails],
                              isNeverMissable: Option[Boolean] = None,
                              genre: Option[String] = None,
                              subGenres: Set[String],
                              tags: Set[String],
                              productionYear: Option[Int] = None)
    extends CanonicalEntity

final case class SeriesDetails(title: String,
                               shortSynopsis: Option[String] = None,
                               mediumSynopsis: Option[String] = None,
                               longSynopsis: Option[String] = None)
    extends EntityDetails

final case class SportEntity(id: EntityId, localizedDetails: Map[Language, SportDetails]) extends CanonicalEntity

final case class SportDetails(name: String) extends EntityDetails

final case class CompetitionEntity(id: EntityId, localizedDetails: Map[Language, CompetitionDetails], sportRef: String)
    extends CanonicalEntity

final case class CompetitionDetails(name: String) extends EntityDetails

final case class TeamEntity(id: EntityId, localizedDetails: Map[Language, TeamDetails], sportRef: String)
    extends CanonicalEntity

final case class TeamDetails(name: String) extends EntityDetails

final case class OfferEntity(id: EntityId,
                             titleId: String,
                             contentSegments: Set[String],
                             startDateTime: OffsetDateTime,
                             endDateTime: OffsetDateTime,
                             language: String,
                             duration: Duration,
                             targetAudienceId: Option[Int] = None,
                             viewingMode: ViewingMode,
                             ownership: OwnershipType = OwnershipType.Unknown,
                             deviceFilters: Set[Device],
                             contentType: ContentType = ContentType.Unknown)
    extends CanonicalEntity

sealed trait ViewingMode extends Product with Serializable

@AvroSortPriority(0) final case class VOD(contentId: String,
                                          proposition: String,
                                          provider: Provider,
                                          regionAvailability: RegionAvailability = Unavailable)
    extends ViewingMode

@AvroSortPriority(1) final case class Linear(
    @AvroDoc("Deprecated, do not include in reader local schemas - use serviceId") serviceKey: String,
    serviceId: Int)
    extends ViewingMode

final case class RegionService(
    @AvroDoc("Deprecated") id: EntityId,
    bouquetId: Int,
    subBouquetId: Int,
    @AvroDoc("Deprecated, do not include in reader local schemas - use serviceId") serviceKey: Int,
    serviceId: Int)
    extends CanonicalEntity

final case class ServiceEntity(id: Int, name: String) extends CanonicalEntity

final case class RegionEntity(id: String, bouquetId: Int, subBouquetId: Int, bouquetType: String)
    extends CanonicalEntity

final case class ContributorEntity(id: EntityId, localizedDetails: Map[Language, PersonDetails]) extends CanonicalEntity

final case class PersonDetails(fullName: String) extends EntityDetails

final case class Credit(entityId: String, contributorId: String, role: Role = Unknown, rank: Int)
    extends CanonicalEntity
