package booking.apps

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import booking.actor.Hotel
import booking.model._
import java.sql.Date
import java.util.UUID
import scala.concurrent.duration._

// the C part from CQRS
object HotelDemo {

  def main(args: Array[String]): Unit = {
    val simpleLogger = Behaviors.receive[Any] { (ctx, message) =>
      ctx.log.info(s"[logger] $message")
      Behaviors.same
    }

    val root = Behaviors.setup[String] { ctx =>
      val logger = ctx.spawn(simpleLogger, "logger") // child actor
      val hotel = ctx.spawn(Hotel("testHotel"), "testHotel")

      hotel ! MakeReservation(UUID.randomUUID().toString, Date.valueOf("2022-07-14"), Date.valueOf("2022-07-21"), 101, logger)
      //hotel ! ChangeReservation("KYH5JJS6CU", Date.valueOf("2022-07-14"), Date.valueOf("2022-07-29"), 101, logger)
      hotel ! CancelReservation("KYH5JJS6CU", logger)
      Behaviors.empty
    }

    val system = ActorSystem(root, "DemoHotel")
    import system.executionContext
    system.scheduler.scheduleOnce(25.seconds, () => system.terminate())
  }

}
