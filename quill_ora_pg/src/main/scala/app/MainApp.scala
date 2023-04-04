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
   pgService <- ZIO.service[PgDataService]
   personsAll <- pgService.getPeopleAll
   _ <-ZIO.foreachDiscard(personsAll){p => ZIO.logInfo(s"Person [PG]: ${p.name} with ${p.age}")}
  } yield ()

  val oraData :ZIO[OraDataService,SQLException,Unit] = for {
    oraService <- ZIO.service[OraDataService]
    personsAll <- oraService.getPeopleAll
    _ <-ZIO.foreachDiscard(personsAll){p => ZIO.logInfo(s"Person [ORA]: ${p.name} with ${p.age}")}
  } yield ()

  val mainApp: ZIO[ZIOAppArgs, Throwable, Unit] = for {
    _ <- pgData.provide(PgDataService.live,
      Quill.Postgres.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("pgConfig")
    )
    _ <- oraData.provide(OraDataService.live,
      Quill.Oracle.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("oraConfig")
    )
  } yield ()

  def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    mainApp.foldZIO(
      err => ZIO.logError(s"Exception - ${err.getMessage}").as(0),
      suc => ZIO.succeed(suc)
    )

}
