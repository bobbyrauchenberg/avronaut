import Dependencies._

name := "cupcat-avro"

version := "0.1"

organization := "CupcatCorp"

scalaVersion := "2.12.9"

sbtVersion := "1.2.8"

configureDependencies()

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(schema)

lazy val root = Project(id = "disco-map", base = file("."))
  .aggregate(aggregatedProjects: _*)

lazy val schema = newModule("schema")

lazy val decoder = newModule("decoder")

def newModule(name: String): Project =
  Project(id = name, base = file(name))

