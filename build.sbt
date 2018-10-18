name := "enums"
organization in ThisBuild := "com.veon.ep"

scalacOptions in ThisBuild += "-Xfatal-warnings"
scalaVersion in ThisBuild := "2.12.7"

val test: Seq[ModuleID] = Seq(
  "org.scalatest" %% "scalatest" % `*` % "test"
)
val argonaut = Seq(
  "io.argonaut" %% "argonaut" % `*`
)

lazy val `case-enum` = project
  .settings(
    libraryDependencies ++= test ++ Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value)
  )
  .enablePlugins(Library)

lazy val `case-enum-spray-json` = project
  .settings(
    libraryDependencies ++= test ++ Seq(
      "io.spray" %% "spray-json" % `*`
    )
  )
  .enablePlugins(Library)
  .dependsOn(`case-enum`)

lazy val `enums` = project.in(file(".")).aggregate(`case-enum`, `case-enum-spray-json`)
enablePlugins(Library)
