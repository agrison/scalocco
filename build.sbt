name := "scalocco"

organization := "me.grison.scalocco"

description := "Scalocco: A Docco implementation in Scala"

homepage := Some(url("http://www.grison.me/scalocco/"))

startYear := Some(2013)

// licenses += ???

version := "1.1.0-SNAPSHOT"

scalaVersion := "2.10.1"

scalacOptions ++= Seq(
  "-unchecked"
, "-deprecation"
, "-feature"
, "-language:implicitConversions"
)
