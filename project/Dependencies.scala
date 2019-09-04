import sbt.Keys._
import sbt._

object Dependencies {

  private val catsDeps =
      "org.typelevel" %% "cats-mtl-core" % "0.2.3" +: Seq(
        "cats-core",
        "cats-macros",
        "cats-kernel",
        "cats-testkit"
      ).map("org.typelevel" %% _ % "1.6.1")

  val refinedDeps = Seq(
      "refined",
      "refined-pureconfig",
      "refined-scalacheck"
    ).map("eu.timepit" %% _ % "0.9.9")


  private val sharedDeps = catsDeps ++ refinedDeps ++ Seq(
    "org.apache.avro"     % "avro"                            % "1.9.0",
    "com.propensive"      % "magnolia_2.12"                   % "0.11.0",
    "com.chuusai"         % "shapeless_2.12"                  % "2.3.3",
    "org.json4s"          %% "json4s-native"                  % "3.6.7",
    "org.scalatest"       %% "scalatest"                      % "3.0.8" % "test",
    "org.scalacheck"      %% "scalacheck"                     % "1.14.0" % Test,
    "com.danielasfregola" %% "random-data-generator-magnolia" % "2.6"     % Test,
    "com.ironcorelabs"    %% "cats-scalatest"                 % "2.4.1"               % Test)

  def configureDependencies(extraDeps: Seq[ModuleID] = Seq.empty): Seq[Def.Setting[Seq[ModuleID]]] = Seq(
    libraryDependencies ++= sharedDeps
  )
      
}
