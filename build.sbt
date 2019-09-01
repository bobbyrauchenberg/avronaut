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

def newModule(name: String): Project =
  Project(id = name, base = file(name))

// The dependencies are in Maven format, with % separating the parts.  
// Notice the extra bit "test" on the end of JUnit and ScalaTest, which will 
// mean it is only a test dependency.
//
// The %% means that it will automatically add the specific Scala version to the dependency name.  
// For instance, this will actually download scalatest_2.9.2

// Initial commands to be run in your REPL.  I like to import various project-specific things here.
