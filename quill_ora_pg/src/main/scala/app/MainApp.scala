package app

import app.MainApp.mainApp
import common.Person
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import pg_service.DataService
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

import java.sql.SQLException

object MainApp extends ZIOAppDefault {

  val pgData :ZIO[DataService,SQLException,Unit] = for {
   _ <- ZIO.logInfo("pgData processing ....")
   pgService <- ZIO.service[DataService]
   _ <- ZIO.logInfo("~ All person ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
   personsAll <- pgService.getPeopleAll
   _ <-ZIO.foreachDiscard(personsAll){p => ZIO.logInfo(s"Person: ${p.name} with ${p.age}")}
   _ <- ZIO.logInfo("~ Person with age >= 30 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
   personsGt30 <- pgService.getPeopleAgeGt(30)
   _ <-ZIO.foreachDiscard(personsGt30){p => ZIO.logInfo(s"Person: ${p.name} with ${p.age}")}
   _ <- ZIO.logInfo("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
  } yield ()

  val mainApp: ZIO[ZIOAppArgs, Throwable, Unit] = for {
    _ <- ZIO.logInfo(s"Begin mainApp")
    _ <- pgData.provide(DataService.live,
      Quill.Postgres.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("pgConfig")
    )
  } yield ()

  def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    mainApp.foldZIO(
      err => ZIO.logError(s"Exception - ${err.getMessage}").as(0),
      suc => ZIO.succeed(suc)
    )

}
/*
  override def run = {
    DataService.getPeople(30)
      .provide(
        DataService.live,
        Quill.Postgres.fromNamingStrategy(SnakeCase),
        Quill.DataSource.fromPrefix("pgConfig")
      )
      .debug("Results")
      .exitCode
  }
*/