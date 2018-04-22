val akkaVersion = "2.5.12"

lazy val root = (project in file(".")).
  settings(
    name := "actor-supervision",
    version := "1.0",
    scalaVersion := "2.12.5",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-remote" % akkaVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-testkit-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
      "org.scalatest" %% "scalatest" % "3.0.4" % "test")
  )
