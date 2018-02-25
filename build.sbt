name := "ArrowPuzzleSolver"

scalaVersion in ThisBuild := "2.12.4"

scalacOptions in ThisBuild ++= Seq(
  "-feature",
  "-deprecation",
  "-Yno-adapted-args",
  "-Ywarn-value-discard",
  "-Ywarn-numeric-widen",
  "-Ywarn-dead-code",
  "-Xlint",
  "-Xfatal-warnings",
  "-unchecked",
  "-language:implicitConversions")

scalacOptions in (Compile, console) ~= (_.filterNot(_ == "-Xlint"))
scalacOptions in (Test, console) ~= (_.filterNot(_ == "-Xlint"))

testOptions in Test += Tests.Setup(classLoader =>
  classLoader
    .loadClass("org.slf4j.LoggerFactory")
    .getMethod("getLogger", classLoader.loadClass("java.lang.String"))
    .invoke(null, "ROOT")
)

libraryDependencies in ThisBuild ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  // "org.slf4j"                  %  "slf4j-api"     % "1.7.7",
  "ch.qos.logback"             %  "logback-classic" % "1.2.3",
  "org.scalatest"              %% "scalatest"     % "3.0.0"  % "test",
  "org.scalacheck"             %% "scalacheck"    % "1.13.4" % "test"
)

initialCommands in (Test, console) := """
import org.saegesser.puzzle._
import org.saegesser.test._
import TestUtil._
"""

initialCommands in (Compile, console) := """
import org.saegesser.puzzle._
"""

parallelExecution in Test := false

lazy val puzzle = project.in(file("."))
