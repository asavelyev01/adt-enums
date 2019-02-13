name := "enums"
organization in ThisBuild := "com.veon.scalalibs"

scalacOptions in ThisBuild += "-Xfatal-warnings"
scalaVersion in ThisBuild := "2.12.7"

val test = Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)
lazy val `case-enum` = project
  .settings(
    libraryDependencies ++= test ++ Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value),
  )

lazy val `case-enum-argonaut` = project
  .settings(
    libraryDependencies ++= test ++ Seq(
      "io.argonaut" %% "argonaut" % "6.2.2" % Provided
    )
  )
  .dependsOn(`case-enum`)

lazy val `case-enum-slick` = project
  .settings(
    libraryDependencies ++= test ++ Seq(
      "com.typesafe.slick" %% "slick" % "3.2.3" % Provided
    )
  )
  .dependsOn(`case-enum`)

lazy val `enums` =
  project
    .in(file("."))
    .aggregate(`case-enum`, `case-enum-argonaut`, `case-enum-slick`)

