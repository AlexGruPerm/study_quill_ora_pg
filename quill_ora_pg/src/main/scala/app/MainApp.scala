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

  val dsPgLayer: ZLayer[Any, Throwable, DataSource] = Quill.DataSource.fromPrefix("pgConfig")
  val quillPgLayer: ZLayer[DataSource, Nothing, Postgres[SnakeCase.type]] = Quill.Postgres.fromNamingStrategy(SnakeCase)
  val dsPgServ: ZLayer[Postgres[SnakeCase], Nothing, PgDataService] = PgDataService.live

  val dsOraLayer: ZLayer[Any, Throwable, DataSource] = Quill.DataSource.fromPrefix("oraConfig")
  val quillOraLayer: ZLayer[DataSource, Nothing, Oracle[SnakeCase.type]] = Quill.Oracle.fromNamingStrategy(SnakeCase)
  val dsOraServ: ZLayer[Oracle[SnakeCase], Nothing, OraDataService] = OraDataService.live

  val pg :ZLayer[Any,Throwable,PgDataService] = dsPgLayer >>> quillPgLayer >>> dsPgServ
  val ora: ZLayer[Any,Throwable,OraDataService] = dsOraLayer >>> quillOraLayer >>> dsOraServ

  val logic :ZIO[PgDataService with OraDataService,SQLException,Unit] = for {
    pgService <- ZIO.service[PgDataService]
    oraService <- ZIO.service[OraDataService]
    personsPgAll <- pgService.getPeopleAll
    personsOraAll <- oraService.getPeopleAll
    _ <- ZIO.foreachDiscard(personsPgAll){p => ZIO.logInfo(s"Person [PG][${p.id}]: ${p.name} with ${p.age}")}
    _ <- ZIO.foreachDiscard(personsOraAll){p => ZIO.logInfo(s"Person [ORA][${p.id}]: ${p.name} with ${p.age}")}
  } yield ()

  val fromPgToOraByAllCols: ZIO[PgDataService with OraDataService,SQLException,Unit] = for {
    pgService <- ZIO.service[PgDataService]
    oraService <- ZIO.service[OraDataService]
    personsPgAll <- pgService.getPeopleAll
    personsOraAll <- oraService.getPeopleAll
    forInsert = personsPgAll diff personsOraAll

    /*
    //ok.1
    _ <- ZIO.foreachDiscard(forInsert)(p => ZIO.logInfo(s"For insert to ORACLE: ${p}"))
    insertedIds <- ZIO.foreach(forInsert)(p => oraService.insertRowWithoutGen(p))
    _ <- ZIO.logInfo(s"Inserted ${insertedIds.size} elements id = [${insertedIds}]")
    */

    _ <- ZIO.foreachDiscard(forInsert)(p => ZIO.logInfo(s"For insert to ORACLE: ${p}"))
    insertedIds <- ZIO.foreach(forInsert)(p => oraService.insertRow(p))
    _ <- ZIO.logInfo(s"Inserted ${insertedIds.size} elements id = [${insertedIds}]")
    /*
    _ <- oraService.insertRows(forInsert).catchAll{
      case ex: SQLException =>
        ZIO.logError(s"Error: ${ex.getMessage} - ${ex.getSQLState} - ${ex.getErrorCode} - ${ex.getCause}")
    }
    */
  } yield ()

  val fromOraToPgByAllCols: ZIO[PgDataService with OraDataService,SQLException,Unit] = for {
    pgService <- ZIO.service[PgDataService]
    oraService <- ZIO.service[OraDataService]
    personsPgAll <- pgService.getPeopleAll
    personsOraAll <- oraService.getPeopleAll
    forInsert = personsOraAll diff personsPgAll
    _ <- ZIO.foreachDiscard(forInsert){p => ZIO.logInfo(s"For insert to ORACLE: ${p}")}
    _ <- pgService.insertRows(forInsert)
    /*
   _ <- ZIO.foreachDiscard(personsPgAll){p => ZIO.logInfo(s"Person [PG][${p.id}]: ${p.name} with ${p.age}")}
   _ <- ZIO.foreachDiscard(personsOraAll){p => ZIO.logInfo(s"Person [ORA][${p.id}]: ${p.name} with ${p.age}")}
   */
  } yield ()

  val mainApp: ZIO[ZIOAppArgs, Throwable, Unit] = for {
    //_ <- logic.provide(pg, ora)
    _ <- fromPgToOraByAllCols.provide(ora,pg)
    //_ <- fromOraToPgByAllCols.provide(pg, ora)
  } yield ()

  def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    mainApp.debug("Result").foldZIO(
      err => ZIO.logError(s"Exception - ${err.getMessage}").as(0),
      suc => ZIO.succeed(suc)
    )

}
