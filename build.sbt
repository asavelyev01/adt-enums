name := "enums"
organization in ThisBuild := "com.veon.ep"

scalacOptions in ThisBuild += "-Xfatal-warnings"
scalaVersion in ThisBuild := "2.12.7"

libraryDependencies ++= Deps.test

lazy val `case-enum` = project
  .settings(
    libraryDependencies ++= Deps.test ++ Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value)
  )
  .enablePlugins(Library)

lazy val `case-enum-spray-json` = project
  .settings(
    libraryDependencies ++= Deps.test ++ Deps.sprayJson
  )
  .enablePlugins(Library)
  .dependsOn(`case-enum`)

lazy val `enums` = project.in(file(".")).aggregate(`case-enum`, `case-enum-spray-json`)
enablePlugins(Library)
