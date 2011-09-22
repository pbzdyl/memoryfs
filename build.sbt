name := "Memory FileSystem"

version := "1.0"

scalaVersion := "2.9.1"

libraryDependencies ++= Seq(
   "org.scala-tools.testing" %% "scalacheck" % "1.9" % "test",
   "org.scala-tools.testing" %% "specs" % "1.6.9" % "test",
   "junit" % "junit" % "4.9" % "test"
)

scalacOptions += "-deprecation"
