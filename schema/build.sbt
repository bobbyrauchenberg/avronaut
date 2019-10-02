import Dependencies._

AvronautBuild.projectSettings

addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary)

configureDependencies()
