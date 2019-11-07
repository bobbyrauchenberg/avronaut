package unit.domain.assembled

import java.time.{Duration, OffsetDateTime}

import com.sksamuel.avro4s.{AvroDoc, AvroNamespace, AvroSortPriority}
import unit.domain.CommonEntityFields.{EntityId, Language}
import unit.domain._

sealed trait AssembledEntity extends Product with Serializable {
  def id: EntityId
}

final case class AssembledOfferEntity(id: EntityId,
                                      contentSegments: Set[String],
                                      startDateTime: OffsetDateTime,
                                      endDateTime: OffsetDateTime,
                                      language: String,
                                      duration: Duration,
                                      targetAudienceId: Option[Int] = None,
                                      viewingMode: ViewingMode,
                                      @AvroNamespace("com.sky.discovery.map.domain.assembled")
                                      ownership: OwnershipType = OwnershipType.Unknown,
                                      @AvroNamespace("com.sky.discovery.map.domain.assembled")
                                      deviceFilters: Set[Device],
                                      title: Title,
                                      @AvroNamespace("com.sky.discovery.map.domain.assembled")
                                      contentType: ContentType = ContentType.Unknown)
    extends AssembledEntity

sealed trait ViewingMode extends Product with Serializable

@AvroSortPriority(0) final case class VOD(contentId: String,
                                          proposition: String,
                                          @AvroNamespace("com.sky.discovery.map.domain.assembled") provider: Provider)
    extends ViewingMode

@AvroSortPriority(1) final case class Linear(@AvroDoc("Deprecated - use service.id") serviceKey: String,
                                             service: Service)
    extends ViewingMode

final case class Service(id: Int, name: String)

final case class AssembledTitleEntity(
    id: EntityId,
    @AvroNamespace("com.sky.discovery.map.domain.assembled.title")
    titleType: TitleType,
    @AvroNamespace("com.sky.discovery.map.domain.assembled.title") localizedDetails: Map[Language, TitleDetails],
    forNeverMiss: Boolean,
    genre: Option[String] = None,
    subGenres: Set[String],
    tags: Set[String],
    @AvroNamespace("com.sky.discovery.map.domain.assembled.title")
    episodic: Option[Either[InSeason, OutOfSeason]] = None,
    productionYear: Option[Int] = None,
    @AvroNamespace("com.sky.discovery.map.domain.assembled.title") ageRatings: Option[AgeRatings] = None)
    extends AssembledEntity

final case class Title(id: String,
                       @AvroNamespace("com.sky.discovery.map.domain.assembled")
                       @AvroDoc("Deprecated in favour of type")
                       titleType: TitleType,
                       localizedDetails: Map[Language, TitleDetails],
                       forNeverMiss: Boolean,
                       genre: Option[String] = None,
                       subGenres: Set[String],
                       tags: Set[String],
                       @AvroDoc("Deprecated in favour of type")
                       @AvroNamespace("com.sky.discovery.map.domain.assembled")
                       episodic: Option[Either[InSeason, OutOfSeason]] = None,
                       productionYear: Option[Int] = None,
                       @AvroNamespace("com.sky.discovery.map.domain.assembled") ageRatings: Option[AgeRatings] = None)

final case class TitleDetails(title: String,
                              shortSynopsis: Option[String] = None,
                              mediumSynopsis: Option[String] = None,
                              longSynopsis: Option[String] = None)

final case class InSeason(episodeNumber: Int, lastInSeason: Boolean, season: Season, series: Series)

final case class Season(id: EntityId,
                        localizedDetails: Map[Language, SeasonDetails],
                        seasonNumber: Int,
                        genre: Option[String] = None,
                        subGenres: Set[String],
                        tags: Set[String],
                        productionYear: Option[Int] = None)

final case class SeasonDetails(title: String,
                               shortSynopsis: Option[String] = None,
                               mediumSynopsis: Option[String] = None,
                               longSynopsis: Option[String] = None)

final case class OutOfSeason(episodeNumber: Option[Int] = None, series: Series)

final case class Series(id: String,
                        localizedDetails: Map[Language, SeriesDetails],
                        isNeverMissable: Option[Boolean] = None,
                        genre: Option[String],
                        subGenres: Set[String],
                        tags: Set[String],
                        productionYear: Option[Int] = None)

final case class SeriesDetails(title: String,
                               shortSynopsis: Option[String] = None,
                               mediumSynopsis: Option[String] = None,
                               longSynopsis: Option[String] = None)
