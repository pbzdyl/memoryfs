name := "Memory File System"

version := "1.0-SNAPSHOT"

scalaVersion := "2.9.1"

libraryDependencies ++= Seq(
//   "org.scala-tools.testing" %% "scalacheck" % "1.9" % "test",
//   "org.scala-tools.testing" %% "specs" % "1.6.9" % "test",
   "org.scala-tools" %% "scala-stm" % "0.4",
   "org.scalatest" %% "scalatest" % "1.6.1" % "test",
   "junit" % "junit" % "4.9" % "test",
   "org.mockito" % "mockito-core" % "1.9.0-rc1"
)

scalacOptions += "-deprecation"
