import scalariform.formatter.preferences._

name          := "progressed.io"

organization  := "io.progressed"

version       := "0.1-SNAPSHOT"

scalaVersion  := "2.11.2"

scalacOptions := Seq(
    "-unchecked",
    "-deprecation",
    "-encoding", "utf8",
    "-feature",
    "-language:higherKinds",
    "-language:postfixOps",
    "-language:implicitConversions",
    "-language:existentials")

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io/")

libraryDependencies ++= {
  val akkaV = "2.3.2"
  val sprayV = "1.3.1"
  Seq(
    "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
    "io.spray"            %%  "spray-can"     % sprayV,
    "io.spray"            %%  "spray-routing" % sprayV,
    "io.spray"            %%  "spray-caching" % sprayV,
    "io.spray"            %%  "spray-testkit" % sprayV,
    "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"  % akkaV)
}

assemblySettings

seq(Revolver.settings: _*)

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignParameters, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
  .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)