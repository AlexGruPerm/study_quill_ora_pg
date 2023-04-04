name := "quill_ora_pg"

ThisBuild / organization := "yakushev"
ThisBuild / version      := "0.0.1"
ThisBuild / scalaVersion := "2.13.10"

  val Versions = new {
    val zio         = "2.0.10"
    val pgVers      = "42.6.0"
    val oraVers     = "12.2.0.1"
    val quillZio    = "4.6.0"
  }

  // PROJECTS
  lazy val global = project
  .in(file("."))
  .settings(commonSettings)
  .disablePlugins(AssemblyPlugin)
  .aggregate(
    quill_ora_pg
  )

  lazy val quill_ora_pg = (project in file("quill_ora_pg"))
  .settings(
    Compile / mainClass        := Some("app.MainApp"),
    assembly / assemblyJarName := "quill_ora_pg.jar",
    name := "quill_ora_pg",
    commonSettings,
    libraryDependencies ++= commonDependencies
  )

  lazy val dependencies =
    new {
      val zio = "dev.zio" %% "zio" % Versions.zio
      val pg = "org.postgresql" % "postgresql" % Versions.pgVers
      val ora = "com.oracle.jdbc" % "ojdbc8" % Versions.oraVers
      val quill_zio = "io.getquill" %% "quill-jdbc-zio" % Versions.quillZio


      val zioDep = List(zio,quill_zio)
      val dbDep = List(pg,ora)
    }

  val commonDependencies = {
    dependencies.zioDep ++ dependencies.dbDep
  }

  lazy val compilerOptions = Seq(
          "-deprecation",
          "-encoding", "utf-8",
          "-explaintypes",
          "-feature",
          "-unchecked",
          "-language:postfixOps",
          "-language:higherKinds",
          "-language:implicitConversions",
          "-Xcheckinit",
          "-Xfatal-warnings",
          "-Ywarn-unused:params,-implicits"
  )

  lazy val commonSettings = Seq(
    scalacOptions ++= compilerOptions,
    resolvers ++= Seq(
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      "Ora driver" at "https://broadinstitute.jfrog.io/artifactory/libs-release-local/",
      Resolver.DefaultMavenRepository,
      Resolver.mavenLocal,
      Resolver.bintrayRepo("websudos", "oss-releases")
    )++
      Resolver.sonatypeOssRepos("snapshots")
     ++ Resolver.sonatypeOssRepos("public")
     ++ Resolver.sonatypeOssRepos("releases")
  )

  quill_ora_pg / assembly / assemblyMergeStrategy := {
    case PathList("module-info.class") => MergeStrategy.discard
    case x if x.endsWith("/module-info.class") => MergeStrategy.discard
    case PathList("META-INF", xs @ _*)         => MergeStrategy.discard
    case "reference.conf" => MergeStrategy.concat
    case _ => MergeStrategy.first
  }