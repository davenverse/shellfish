ThisBuild / tlBaseVersion := "0.0"

ThisBuild / startYear := Some(2024)
ThisBuild / licenses  := Seq(License.MIT)
ThisBuild / developers := List(
  tlGitHubDev("ChristopherDavenport", "Christopher Davenport")
)

ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("17"))
ThisBuild / crossScalaVersions         := Seq("2.13.14", "3.3.3")
ThisBuild / tlJdkRelease               := Some(11)

ThisBuild / tlSitePublishBranch := Some("main")

// Projects
lazy val shellfish = tlCrossRootProject
  .aggregate(core, examples)

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(
    name := "shellfish",
    libraryDependencies ++= List(
      "org.typelevel"  %% "cats-core"         % "2.12.0",
      "org.typelevel"  %% "alleycats-core"    % "2.12.0",
      "org.typelevel"  %% "cats-effect"       % "3.5.4",
      "co.fs2"         %% "fs2-core"          % "3.10.2",
      "co.fs2"         %% "fs2-io"            % "3.10.2",
      "org.typelevel" %%% "munit-cats-effect" % "2.0.0" % Test
    )
  )

lazy val examples = project
  .in(file("examples"))
  .enablePlugins(NoPublishPlugin)
  .dependsOn(core.jvm)
  .settings(
    name                 := "shellfish-examples",
    Compile / run / fork := true
  )

lazy val site = project
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .dependsOn(core.jvm)
