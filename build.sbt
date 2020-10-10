name := "enums"
organization in ThisBuild := "com.asavelyev"

scalacOptions in ThisBuild += "-Xfatal-warnings"
scalaVersion in ThisBuild := "2.13.3"
crossScalaVersions in ThisBuild := Seq(scalaVersion.value, "2.12.12")

val test = Seq(
  "org.scalatest" %% "scalatest" % "3.2.0" % "test"
)
lazy val `case-enum` = project
  .settings(
    libraryDependencies ++= test ++ Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value),
  )

lazy val `case-enum-argonaut` = project
  .settings(
    libraryDependencies ++= test ++ Seq(
      "io.argonaut" %% "argonaut" % "6.3.1" % Provided
    )
  )
  .dependsOn(`case-enum`)

lazy val `case-enum-slick` = project
  .settings(
    libraryDependencies ++= test ++ Seq(
      "com.typesafe.slick" %% "slick" % "3.3.3" % Provided
    )
  )
  .dependsOn(`case-enum`)

lazy val `enums` =
  project
    .in(file("."))
    .aggregate(`case-enum`, `case-enum-argonaut`, `case-enum-slick`)

