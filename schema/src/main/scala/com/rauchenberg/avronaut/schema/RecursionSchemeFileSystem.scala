//package com.rauchenberg.avronaut.schema
//
//import matryoshka.{Algebra, Corecursive}
//import matryoshka.data.Fix
//import scalaz.Functor
//
//sealed trait EntryF[+F]
//final case class Dir[F](files: Map[String, F]) extends EntryF[F]
//final case class File(size: Long)              extends EntryF[Nothing]
//
//object EntryF {
//  implicit val functor: Functor[EntryF] = new Functor[EntryF] {
//    override def map[A, B](fa: EntryF[A])(f: A => B): EntryF[B] = fa match {
//      case f: File => f
//      case Dir(fs) => Dir(fs.map { case (k, v) => (k, f(v)) })
//    }
//  }
//
//  val totalFileSize: Algebra[EntryF, Long] = {
//    case File(s) => s
//    case Dir(fs) => fs.values.sum
//  }
//
//  val countFiles: Algebra[EntryF, Int] = {
//    case File(_) => 1
//    case Dir(fs) => fs.values.sum
//  }
//
//  val countDirs: Algebra[EntryF, Int] = {
//    case File(_) => 0
//    case Dir(fs) => fs.values.sum + 1
//  }
//
//  def algebraZip[F[_], A, B](fa: Algebra[F, A], fb: Algebra[F, B])(implicit F: Functor[F]): Algebra[F, (A, B)] =
//    fab => {
//      val a = fa(F.map(fab)(_._1))
//      val b = fb(F.map(fab)(_._2))
//      (a, b)
//    }
//
//  val statsAlg: Algebra[EntryF, (Long, (Int, (Int)))] = algebraZip(totalFileSize, algebraZip(countFiles, countDirs))
//
//}
//
//object Entry extends App {
//
//  type Entry = Fix[EntryF]
//
//  def apply(f: EntryF[Entry]): Entry   = Fix(f)
//  def file(s: Long): Entry             = apply(File(s))
//  def dir(es: (String, Entry)*): Entry = apply(Dir(es.toMap))
//
//  val example: Entry =
//    Entry.dir("usr" -> Entry.dir("bin"         -> Entry.dir("find" -> Entry.file(197360), "ls" -> Entry.file(133688))),
//              "tmp" -> Entry.dir("example.tmp" -> Entry.file(12)))
//
//  import matryoshka.implicits._
//
//  def stats(e: Entry): Stats = {
//    val (totalSize, (files, dirs)) = e.cata(EntryF.statsAlg)
//    Stats(totalSize, files, dirs)
//  }
//
//  println(stats(example))
//
//}
//
//final case class Stats(totalSize: Long, files: Int, dirs: Int)
//
//object FileSystem {}
