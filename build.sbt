import Dependencies._

scalafmtVersion in ThisBuild := "1.2.0"
scalafmtOnCompile in ThisBuild := true

configureDependencies()

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(common, decoder, schema)

lazy val root = Project(id = "cupcat-avro", base = file("."))
  .aggregate(aggregatedProjects: _*)

lazy val common = newModule("common")

lazy val schema = newModule("schema").dependsOn(
  common % "compile->compile"
)

lazy val decoder = newModule("decoder").dependsOn(
  common % "compile->compile",
  schema % "test->compile"
)

def newModule(name: String): Project =
  Project(id = name, base = file(name))
    .settings(AvroCupcatBuild.buildSettings)
