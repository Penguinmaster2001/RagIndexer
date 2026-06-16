val scala3Version = "3.8.4"



lazy val root = project
    .in(file("."))
    .settings(
      name := "RagIndexer",
      version := "0.1.0-SNAPSHOT",

      scalaVersion := scala3Version,

      libraryDependencies ++= Seq(
        "com.lihaoyi" %% "requests" % "0.9.3",
        "com.lihaoyi" %% "os-lib" % "0.11.8",
        "io.circe" %% "circe-core"           % "0.14.15",
        "io.circe" %% "circe-parser"         % "0.14.15",
        "io.circe" %% "circe-generic"        % "0.14.15",
        "io.circe" %% "circe-generic-extras" % "0.14.5-RC1",
        "org.scalameta" %% "munit" % "1.3.2" % Test
      )
    )
