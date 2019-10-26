import Dependencies._

scalafmtVersion in ThisBuild := "1.2.0"
scalafmtOnCompile in ThisBuild := true

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary)

configureDependencies()

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(core, benchmarks)

lazy val root = Project(id = "avronaut", base = file("."))
  .aggregate(aggregatedProjects: _*)
  .enablePlugins(JmhPlugin)

lazy val core = newModule("core")

lazy val benchmarks = newModule("benchmark")
  .enablePlugins(JmhPlugin)
  .dependsOn(core % "test -> test;test -> compile")

def newModule(name: String): Project =
  Project(id = name, base = file(name))
    .settings(AvronautBuild.buildSettings)
