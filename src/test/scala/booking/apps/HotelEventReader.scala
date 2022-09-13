package booking.apps

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.alpakka.cassandra.scaladsl.CassandraSessionRegistry
import akka.stream.scaladsl.{Sink, Source}
import booking.model._

import java.time.temporal.ChronoUnit
import scala.concurrent.Future

// the Q part from CQRS
object HotelEventReader {

  implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "HotelEventReader")

  import system.executionContext

  // read journal
  val readJournal = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  // C* session
//  val session = CassandraSessionRegistry(system).sessionFor("akka.projection.cassandra.session-config")

  // all persistence IDs
  val persistenceIds: Source[String, NotUsed] = readJournal.persistenceIds()
  val consumptionSink = Sink.foreach(println)
  val persistenceIdsGraph = persistenceIds.to(consumptionSink)

  // all events for a persistence ID
  val eventsForTestHotel = readJournal
    .eventsByPersistenceId("testHotel", 0, Long.MaxValue)
    .map(_.event)
    .map {
      case ReservationAccepted(res) =>
        println(s"MAKING RESERVATION: $res")
        //makeReservation(res)
      case ReservationUpdated(oldReservation, newReservation) =>
        println(s"CHANGING RESERVATION: from $oldReservation to $newReservation")
//        for {
//          _ <- removeReservation(oldReservation)
//          _ <- makeReservation(newReservation)
//        } yield ()
      case ReservationCanceled(res) =>
        println(s"CANCELLING RESERVATION: $res")
        //removeReservation(res)
    }

  def main(args: Array[String]): Unit = {
    //persistenceIdsGraph.run()
    eventsForTestHotel.to(Sink.ignore).run()
  }
}
