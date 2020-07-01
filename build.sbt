val dottyVersion =  "0.25.0-RC2"

val pubSettings = Seq(
  publishMavenStyle := true,
  bintrayOrganization := Some("blocke"),
  bintrayReleaseOnPublish in ThisBuild := true,
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  bintrayRepository := "releases",
  bintrayPackageLabels := Seq("scala", "dotty", "reflection")
)

resolvers += "co.blocke provisional resolver" at "https://dl.bintray.com/blocke/provisional"

lazy val root = project
  .in(file("."))
  .settings(pubSettings: _*)
  .settings(
    name := "dotty-reflection",

    organization := "co.blocke",
    
    resolvers += Resolver.jcenterRepo,

    doc := null,  // disable dottydoc for now

    sources in (Compile, doc) := Seq(),

    scalaVersion := dottyVersion,

    Test / parallelExecution := false,

    // scalacOptions ++= Seq("-language:implicitConversions","-Xprint:typer"),
    scalacOptions ++= Seq("-language:implicitConversions"),

    testFrameworks += new TestFramework("munit.Framework"),

    libraryDependencies ++= 
      Seq("ch.epfl.lamp" %% "dotty-compiler" % dottyVersion,
      "ch.epfl.lamp" %% "dotty-tasty-inspector" % dottyVersion,
      "ch.epfl.lamp" %% "tasty-core" % dottyVersion,
      "org.scalameta" %% "munit" % "0.7.9" % Test
      )
  )
