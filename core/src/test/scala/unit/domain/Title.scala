package unit.domain

import com.sksamuel.avro4s.AvroSortPriority

object Title {

  sealed trait Type extends Product with Serializable

  @AvroSortPriority(0) case object Unknown             extends Type
  @AvroSortPriority(1) case object Movie               extends Type
  @AvroSortPriority(2) case object StandaloneProgramme extends Type
  @AvroSortPriority(3) final case class InSeason(seasonId: String,
                                                 episode: Either[SpecialEpisode.type, InSeasonEpisode])
      extends Type
  @AvroSortPriority(4) final case class OutOfSeason(seriesId: String,
                                                    episode: Either[SpecialEpisode.type, OutOfSeasonEpisode])
      extends Type

  final case class InSeasonEpisode(episodeNumber: Int, lastInSeason: Boolean)
  final case class OutOfSeasonEpisode(episodeNumber: Int)
  case object SpecialEpisode

}
