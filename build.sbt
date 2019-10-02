import Dependencies._

scalafmtVersion in ThisBuild := "1.2.0"
scalafmtOnCompile in ThisBuild := true

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary)

configureDependencies()

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(common, decoder, schema, encoder)

lazy val root = Project(id = "avronaut", base = file("."))
  .aggregate(aggregatedProjects: _*)

lazy val common = newModule("common")

lazy val schema = newModule("schema").dependsOn(
  common % "compile->compile"
)

lazy val decoder = newModule("decoder").dependsOn(
  common % "compile->compile; test->test",
  schema % "compile->compile; test->compile"
)

lazy val encoder = newModule("encoder").dependsOn(
  common  % "compile->compile; test->test",
  schema  % "compile->compile; test->compile",
  decoder % "test->compile"
)

def newModule(name: String): Project =
  Project(id = name, base = file(name))
    .settings(AvronautBuild.buildSettings)
