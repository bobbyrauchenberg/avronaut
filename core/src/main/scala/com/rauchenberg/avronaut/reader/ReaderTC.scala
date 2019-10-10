package com.rauchenberg.avronaut.reader

import cats.data.Reader
import magnolia.{CaseClass, Magnolia}

trait ReaderTC[A] {

  def runReader: Reader[Int, List[String]]

}

object ReaderTC {

  def apply[A](implicit rtc: ReaderTC[A]) = rtc.runReader

  type Typeclass[A] = ReaderTC[A]

  implicit def gen[A]: Typeclass[A] = macro Magnolia.gen[A]

  def combine[A](ctx: CaseClass[Typeclass, A]): Typeclass[A] = new Typeclass[A] {
    override def runReader: Reader[Int, List[String]] =
      ctx.parameters.foldLeft(Reader((_: Int) => List(""))) {
        case (acc, tc) =>
          for {
            a <- acc
            b <- tc.typeclass.runReader
          } yield a ++ b
      }
  }

  implicit val intReader = new ReaderTC[Int] {
    override def runReader: Reader[Int, List[String]] =
      Reader((i: Int) => List((i * 5).toString))
  }

  implicit val stringReader = new ReaderTC[String] {
    override def runReader: Reader[Int, List[String]] =
      Reader((i: Int) => List((i - 10).toString))
  }
}

object M extends App {

  val tc = ReaderTC[X]

  println(tc.run.apply(100))

  case class X(field1: Int, field2: String, field3: Int)

}
