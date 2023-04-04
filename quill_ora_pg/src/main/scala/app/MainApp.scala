package app

import app.MainApp.mainApp
import common.Person
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import io.getquill.jdbczio.Quill.{Oracle, Postgres}
import ora_service.OraDataService
import pg_service.PgDataService
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

import java.sql.SQLException
import javax.sql.DataSource

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

  val logic :ZIO[PgDataService with OraDataService,SQLException,Unit] = for {
    pgService <- ZIO.service[PgDataService]
    oraService <- ZIO.service[OraDataService]
    personsPgAll <- pgService.getPeopleAll
    personsOraAll <- oraService.getPeopleAll
    _ <- ZIO.foreachDiscard(personsPgAll){p => ZIO.logInfo(s"Person [PG]: ${p.name} with ${p.age}")}
    _ <- ZIO.foreachDiscard(personsOraAll){p => ZIO.logInfo(s"Person [ORA]: ${p.name} with ${p.age}")}
  } yield ()

  val dsPgLayer: ZLayer[Any, Throwable, DataSource] = Quill.DataSource.fromPrefix("pgConfig")
  val quillPgLayer: ZLayer[DataSource, Nothing, Postgres[SnakeCase.type]] = Quill.Postgres.fromNamingStrategy(SnakeCase)
  val dsPgServ: ZLayer[Postgres[SnakeCase], Nothing, PgDataService] = PgDataService.live

  val dsOraLayer: ZLayer[Any, Throwable, DataSource] = Quill.DataSource.fromPrefix("oraConfig")
  val quillOraLayer: ZLayer[DataSource, Nothing, Oracle[SnakeCase.type]] = Quill.Oracle.fromNamingStrategy(SnakeCase)
  val dsOraServ: ZLayer[Oracle[SnakeCase], Nothing, OraDataService] = OraDataService.live


  val pg :ZLayer[Any,Throwable,PgDataService] = dsPgLayer >>> quillPgLayer >>> dsPgServ
  val ora: ZLayer[Any,Throwable,OraDataService] = dsOraLayer >>> quillOraLayer >>> dsOraServ

  val mainApp: ZIO[ZIOAppArgs, Throwable, Unit] = for {
/*    _ <- pgData.provide(
      dsPgServ,
      quillPgLayer,
      dsPgLayer
    )
    _ <- oraData.provide(
      dsOraServ,
      quillOraLayer,
      dsOraLayer
    )*/

    _ <- logic.provide(
      pg,
      ora
    )

  } yield ()

  def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    mainApp.foldZIO(
      err => ZIO.logError(s"Exception - ${err.getMessage}").as(0),
      suc => ZIO.succeed(suc)
    )

}
