name := "enums"
organization in ThisBuild := "com.veon.scalalibs"

scalacOptions in ThisBuild += "-Xfatal-warnings"
scalaVersion in ThisBuild := "2.12.7"

val test = Seq(
  "org.scalatest" ^^ "scalatest" ^ "test"
)
lazy val `case-enum` = project
  .settings(
    libraryDependencies += ("org.scala-lang" % "scala-reflect" % scalaVersion.value),
    versionedLibraryDependencies ++= test
  )
  .enablePlugins(Library)

lazy val `case-enum-argonaut` = project
  .settings(
    versionedLibraryDependencies ++= test ++ Seq(
      "io.argonaut" ^^ "argonaut"
    )
  )
  .enablePlugins(Library)
  .dependsOn(`case-enum`)

lazy val `case-enum-slick` = project
  .settings(
    versionedLibraryDependencies ++= test ++ Seq(
      "com.typesafe.slick" ^^ "slick"
    )
  )
  .enablePlugins(Library)
  .dependsOn(`case-enum`)

lazy val `enums` =
  project
    .in(file("."))
    .aggregate(`case-enum`, `case-enum-argonaut`, `case-enum-slick`)
    .enablePlugins(Library)
