package unit

import java.time.{Duration, OffsetDateTime, ZoneOffset}

import com.fortysevendeg.scalacheck.datetime.jdk8.ArbitraryJdk8
import com.fortysevendeg.scalacheck.datetime.jdk8.GenJdk8._
import com.fortysevendeg.scalacheck.datetime.jdk8.granularity.seconds
import org.scalacheck.{Arbitrary, Gen}

object TimeArbitraries {
  implicit val arbOffsetDateTime: Arbitrary[OffsetDateTime] = Arbitrary(
    genZonedDateTime.map(_.withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime))

  implicit val arbLongModestDuration: Arbitrary[Long] =
    Arbitrary(Gen.choose(0, 1000))

  implicit val arbDuration: Arbitrary[Duration] =
    Arbitrary(ArbitraryJdk8.genDuration)
}
