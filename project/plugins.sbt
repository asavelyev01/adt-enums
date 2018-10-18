addSbtPlugin("com.veon.sbt" % "sbt-veon" % "11.1.3-18-g68535cc")

resolvers ++= Seq(
  "Knoopje Releases" at "https://repo.knoopje.com/repository/sbt-plugins/"
)
credentials ++= Seq(Credentials(Path.userHome / ".ivy2" / ".credentials"))
