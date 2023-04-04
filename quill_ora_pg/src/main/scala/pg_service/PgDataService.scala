package pg_service

import common.Person
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.{ZIO, ZLayer}

import java.sql.SQLException

class PgDataService(quill: Quill.Postgres[SnakeCase]) {
  import quill._
  def getPeopleAll: ZIO[Any, SQLException, List[Person]] = run(query[Person])
  def getPeopleAgeGt(ageGt: Int): ZIO[Any, SQLException, List[Person]] = run(query[Person].filter(_.age >= lift(ageGt)))
}

object PgDataService {
  /**
   * For each function in DataService class we have the cover here in compaion object.
  */
  def getPeopleAll: ZIO[PgDataService, SQLException, List[Person]] =
    ZIO.serviceWithZIO[PgDataService](_.getPeopleAll)

  def getPeopleAgeGt(ageGt: Int): ZIO[PgDataService, SQLException, List[Person]] =
    ZIO.serviceWithZIO[PgDataService](_.getPeopleAgeGt(ageGt))

  val live: ZLayer[Quill.Postgres[SnakeCase],Nothing,PgDataService] = ZLayer.fromFunction(new PgDataService(_))
}