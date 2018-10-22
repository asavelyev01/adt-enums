addSbtPlugin("com.veon.sbt" % "sbt-veon" % "11.1.3-23-g8baea4a")

resolvers ++= Seq(
  "Knoopje Releases" at "https://repo.knoopje.com/repository/sbt-plugins/"
)
credentials ++= Seq(Credentials(Path.userHome / ".ivy2" / ".credentials"))
