import Dependencies._

configureDependencies()

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

scalacOptions in Compile ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Xfatal-warnings",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-language:experimental.macros",
  "-language:higherKinds",
  "-target:jvm-1.8",
  "-feature",
  "-Ypartial-unification"
)