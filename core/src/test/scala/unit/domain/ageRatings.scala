package unit.domain

final case class AgeRatings(kids: Kids)
final case class Kids(segmentCodes: Set[String])
