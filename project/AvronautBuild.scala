import sbt.Keys._
import sbt._


object AvronautBuild {

  lazy val buildSettings = Seq(
    organization := "com.rauchenberg.avronaut",
    scalaVersion := "2.12.9"
  )

  def isTravis = System.getenv("TRAVIS") == "true"

  def travisBuildNumber = System.getenv("TRAVIS_BUILD_NUMBER")

  lazy val projectSettings = buildSettings ++ Seq(
    resolvers ++= Seq(Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")),
    scalacOptions in Compile ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-unchecked",
      "-Ywarn-dead-code",
      "-Ywarn-unused:imports",
      "-Ywarn-unused:locals",
      "-Ywarn-unused:params",
      "-Ywarn-unused:patvars",
      "-Ywarn-unused:privates",
      "-Xfatal-warnings",
      "-language:implicitConversions",
      "-language:postfixOps",
      "-language:experimental.macros",
      "-language:higherKinds",
      "-target:jvm-1.8",
      "-feature",
      "-Ypartial-unification"
    ))
 
}
