package common

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import scalacheckmagnolia.MagnoliaArbitrary._
import unit.{SimpleCase, SimpleInt, SimpleString}

package object common {

  implicit val simpleCaseArbs: Arbitrary[SimpleCase] = Arbitrary {
    Gen.oneOf(
      arbitrary[SimpleString],
      arbitrary[SimpleInt]
    )
  }


}
