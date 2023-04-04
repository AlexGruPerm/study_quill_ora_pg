package ora_service

import common.Person
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.{ZIO, ZLayer}

import java.sql.SQLException

class  OraDataService(quill: Quill.Oracle[SnakeCase]) {
  import quill._
  def getPeopleAll: ZIO[Any, SQLException, List[Person]] = run(query[Person])
  def getPeopleAgeGt(ageGt: Int): ZIO[Any, SQLException, List[Person]] = run(query[Person].filter(_.age >= lift(ageGt)))
}

object  OraDataService {
  /**
   * For each function in DataService class we have the cover here in compaion object.
   */
  def getPeopleAll: ZIO[ OraDataService, SQLException, List[Person]] =
    ZIO.serviceWithZIO[ OraDataService](_.getPeopleAll)

  def getPeopleAgeGt(ageGt: Int): ZIO[ OraDataService, SQLException, List[Person]] =
    ZIO.serviceWithZIO[ OraDataService](_.getPeopleAgeGt(ageGt))

  val live: ZLayer[Quill.Oracle[SnakeCase],Nothing, OraDataService] = ZLayer.fromFunction(new  OraDataService(_))
}