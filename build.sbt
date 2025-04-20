Global / excludeLintKeys += logManager
Global / excludeLintKeys += scalaJSUseMainModuleInitializer
Global / excludeLintKeys += scalaJSLinkerConfig

inThisBuild(
  List(
    organization := "com.indoorvivants",
    organizationName := "Andi Miller and contributors",
    homepage := Some(
      url("https://github.com/indoorvivants/decline-completion")
    ),
    startYear := Some(2023),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        id = "andimiller",
        name = "Andi Miller",
        email = "andi@andimiller.net",
        url = url("http://andimiller.net")
      )
    )
  )
)

// https://github.com/cb372/sbt-explicit-dependencies/issues/27
lazy val disableDependencyChecks = Seq(
  unusedCompileDependenciesTest := {},
  undeclaredCompileDependenciesTest := {}
)

val Scala213 = "2.13.16"
val Scala3 = "3.3.5"
val scalaVersions = Seq(Scala3, Scala213)

lazy val munitSettings = Seq(
  libraryDependencies += {
    "org.scalameta" %%% "munit" % "1.1.0" % Test
  },
  testFrameworks += new TestFramework("munit.Framework")
)

lazy val root = project
  .aggregate(core.projectRefs*)
  .settings(publish / skip := true, publishLocal / skip := true)

lazy val core = projectMatrix
  .in(file("modules/core"))
  .settings(
    name := "decline-completion",
    libraryDependencies ++= Seq(
      "com.monovore" %%% "decline" % "2.5.0",
      "org.typelevel" %% "cats-core" % "2.12.0",
      "org.typelevel" %% "cats-kernel" % "2.12.0"
    )
  )
  .settings(munitSettings)
  .jvmPlatform(scalaVersions)
  .jsPlatform(scalaVersions, disableDependencyChecks)
  .nativePlatform(scalaVersions, disableDependencyChecks)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule))
  )
  .settings(
    snapshotsPackageName := "net.andimiller.decline.completion",
    snapshotsIntegrations += SnapshotIntegration.MUnit // if using MUnit
  )
  .enablePlugins(SnapshotsPlugin)

lazy val docs = project
  .in(file("myproject-docs"))
  .settings(
    scalaVersion := Scala213,
    mdocVariables := Map(
      "VERSION" -> version.value
    ),
    publish / skip := true,
    publishLocal / skip := true
  )
  .settings(disableDependencyChecks)
  .dependsOn(core.jvm(Scala213))
  .enablePlugins(MdocPlugin)

val scalafixRules = Seq(
  "OrganizeImports",
  "DisableSyntax",
  "LeakingImplicitClassVal",
  "NoValInForComprehension"
).mkString(" ")

val CICommands = Seq(
  "clean",
  "scalafixEnable",
  "compile",
  "test",
  "docs/mdoc",
  "scalafmtCheckAll",
  "scalafmtSbtCheck",
  s"scalafix --check $scalafixRules",
  "headerCheck",
  "undeclaredCompileDependenciesTest",
  "unusedCompileDependenciesTest"
).mkString(";")

val PrepareCICommands = Seq(
  "scalafixEnable",
  s"scalafix --rules $scalafixRules",
  "scalafmtAll",
  "scalafmtSbt",
  "headerCreate",
  "undeclaredCompileDependenciesTest"
).mkString(";")

addCommandAlias("ci", CICommands)

addCommandAlias("preCI", PrepareCICommands)
