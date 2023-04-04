
case class Person(id: Int, name: String, age: Int)

val pg: List[Person] = List(
  Person(4,"Joe ",20),
  Person(5," Bloggs",25),
  Person(6,"John",30),
  Person(7,"Smith",35)
)

val ora: List[Person] = List(
  Person(3,"Mark ",10),
  Person(7,"Smith",35)
)

val diff1: List[Person] = pg diff ora

//val diff2: List[Person] = ora.filterNot(pg).contains()