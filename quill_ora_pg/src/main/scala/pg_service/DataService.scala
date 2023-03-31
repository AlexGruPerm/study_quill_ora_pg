package pg_service

import common.Person
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.{ZIO, ZLayer}

import java.sql.SQLException

class DataService(quill: Quill.Postgres[SnakeCase]) {
  import quill._
  def getPeopleAll: ZIO[Any, SQLException, List[Person]] = run(query[Person])
  def getPeopleAgeGt(ageGt: Int): ZIO[Any, SQLException, List[Person]] = run(query[Person].filter(_.age >= lift(ageGt)))
}

object DataService {
  /**
   * For each function in DataService class we have the cover here in compaion object.
  */
  def getPeopleAll: ZIO[DataService, SQLException, List[Person]] =
    ZIO.serviceWithZIO[DataService](_.getPeopleAll)

  def getPeopleAgeGt(ageGt: Int): ZIO[DataService, SQLException, List[Person]] =
    ZIO.serviceWithZIO[DataService](_.getPeopleAgeGt(ageGt))

  val live: ZLayer[Quill.Postgres[SnakeCase],Nothing,DataService] = ZLayer.fromFunction(new DataService(_))
    //todo: ??? ZLayer.fromFunction(q: Quill.Postgres[SnakeCase] => new DataService(q))
}