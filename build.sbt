import Dependencies._

scalafmtVersion in ThisBuild := "1.2.0"
scalafmtOnCompile in ThisBuild := true

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary)

configureDependencies()

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(common)

lazy val root = Project(id = "avronaut", base = file("."))
  .aggregate(aggregatedProjects: _*)

lazy val common = newModule("common")

lazy val core = newModule("core")

def newModule(name: String): Project =
  Project(id = name, base = file(name))
    .settings(AvronautBuild.buildSettings)
