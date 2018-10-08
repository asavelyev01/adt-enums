import sbt._

object Versions {
  val scalaTestVersion = "3.0.5"
  val spray = "1.3.4"
  val argonaut = "6.2.2"
}

object Deps {
  val test: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % Versions.scalaTestVersion % "test"
  )
  val sprayJson = Seq(
    "io.spray" %% "spray-json" % Versions.spray
  )
  val argonaut = Seq(
    "io.argonaut" %% "argonaut" % Versions.argonaut
  )

}
