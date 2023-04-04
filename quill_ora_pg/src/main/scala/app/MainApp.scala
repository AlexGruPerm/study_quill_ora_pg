package app

import app.MainApp.mainApp
import common.Person
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import ora_service.OraDataService
import pg_service.PgDataService
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

import java.sql.SQLException

object MainApp extends ZIOAppDefault {

  val pgData :ZIO[PgDataService,SQLException,Unit] = for {
   _ <- ZIO.logInfo("pgData processing ....")
   pgService <- ZIO.service[PgDataService]
   _ <- ZIO.logInfo("~ All person ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
   personsAll <- pgService.getPeopleAll
   _ <-ZIO.foreachDiscard(personsAll){p => ZIO.logInfo(s"Person: ${p.name} with ${p.age}")}
   _ <- ZIO.logInfo("~ Person with age >= 30 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
   personsGt30 <- pgService.getPeopleAgeGt(30)
   _ <-ZIO.foreachDiscard(personsGt30){p => ZIO.logInfo(s"Person: ${p.name} with ${p.age}")}
   _ <- ZIO.logInfo("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
  } yield ()

  val oraData :ZIO[OraDataService,SQLException,Unit] = for {
    _ <- ZIO.logInfo("pgData processing ....")
    pgService <- ZIO.service[OraDataService]
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
    _ <- ZIO.logInfo(s"========== POSTGRES ========== ")
    _ <- pgData.provide(PgDataService.live,
      Quill.Postgres.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("pgConfig")
    )
    _ <- ZIO.logInfo(s"========== ORACLE ========== ")
    _ <- oraData.provide(OraDataService.live,
      Quill.Postgres.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("oraConfig")
    )
  } yield ()

  def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    mainApp.foldZIO(
      err => ZIO.logError(s"Exception - ${err.getMessage}").as(0),
      suc => ZIO.succeed(suc)
    )

}
