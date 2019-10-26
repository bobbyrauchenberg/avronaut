package unit.common

import org.scalacheck.{Arbitrary, Gen}

object SizedArbitraries {

  implicit val arbChar: Arbitrary[Char] = Arbitrary(Gen.alphaNumChar)

  implicit def stringOfN(size: Int)(implicit arbChar: Arbitrary[Char]): Gen[String] =
    Gen.containerOfN[Array, Char](size, arbChar.arbitrary).map(new String(_))

  implicit def listOfN[A](size: Int)(implicit gen: Gen[A]): Gen[List[A]] = Gen.listOfN(size, gen)

}
