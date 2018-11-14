addSbtPlugin("com.veon.sbt" % "sbt-veon" % "12.0.2")

resolvers ++= Seq(
  "Knoopje Releases" at "https://repo.knoopje.com/repository/sbt-plugins/"
)
credentials ++= Seq(Credentials(Path.userHome / ".ivy2" / ".credentials"))
