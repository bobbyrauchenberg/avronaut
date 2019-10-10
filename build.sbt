import Dependencies._

scalafmtVersion in ThisBuild := "1.2.0"
scalafmtOnCompile in ThisBuild := true

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary)

configureDependencies()

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(core)

lazy val root = Project(id = "avronaut", base = file("."))
  .aggregate(aggregatedProjects: _*)
  .enablePlugins(JmhPlugin)

lazy val core = newModule("core").enablePlugins(JmhPlugin)

def newModule(name: String): Project =
  Project(id = name, base = file(name))
    .settings(AvronautBuild.buildSettings)
