lazy val root = (project in file(".")).
  settings(
    name := "sbt-bootstrap",
    version := "1.0",
    scalaVersion := "2.12.1",

    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.0.1",
      "com.typesafe.akka" %% "akka-http" % "10.0.5",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.5",
      "com.amazonaws" % "aws-java-sdk" % "1.11.118",
      "joda-time" % "joda-time" % "2.9.9",
      "com.twilio.sdk" % "twilio" % "7.8.0",
      "com.typesafe" % "config" % "1.3.1"
    )
  )
