ThisBuild / tlBaseVersion := "0.0" // your current series x.y

ThisBuild / organization     := "cats"
ThisBuild / organizationName := "Typelevel"
ThisBuild / startYear        := Some(2024)
ThisBuild / licenses         := Seq(License.MIT)
ThisBuild / developers := List(
  tlGitHubDev("ChristopherDavenport", "Christopher Davenport"),
  tlGitHubDev("lenguyenthanh", "Thanh Le"),
  tlGitHubDev("armanbilge", "Arman Bilge")
)

// Use JDK 22
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("17"))

ThisBuild / tlSonatypeUseLegacyHost := false

ThisBuild / tlJdkRelease := Option(9)

val Scala3   = "3.4.2"
val Scala213 = "2.13.14"

//ThisBuild / crossScalaVersions := Seq(Scala213, Scala3)
ThisBuild / scalaVersion := Scala3

// Core
val catsV       = "2.12.0"
val catsEffectV = "3.5.4"
val fs2V        = "3.10.2"

// Testing
val munitCatsEffectV = "2.0.0-RC1"

// For Scala 2
val kindProjectorV    = "0.13.3"
val betterMonadicForV = "0.3.1"

// Projects
lazy val `shellfish` = (project in file("."))
  .aggregate(core, examples)

lazy val core = (project in file("core"))
  .settings(
    name := "shellfish",
    libraryDependencies ++= List(
      "org.typelevel"  %% "cats-core"         % catsV,
      "org.typelevel"  %% "alleycats-core"    % catsV,
      "org.typelevel"  %% "cats-effect"       % catsEffectV,
      "co.fs2"         %% "fs2-core"          % fs2V,
      "co.fs2"         %% "fs2-io"            % fs2V,
      "org.typelevel" %%% "munit-cats-effect" % munitCatsEffectV % Test
    )
  )

lazy val examples = (project in file("examples"))
  .enablePlugins(NoPublishPlugin)
  .dependsOn(core)
  .settings(
    name := "shelfish-examples"
  )
