package booking.actor

import akka.actor.typed.Behavior
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import booking.model._

//primary actor talking to cassandra
object Hotel {

  case class State(reservations: Set[Reservation])

  def commandHandler(hotelId: String): (State, Command) => Effect[Event, State] = (state, command) =>
    command match {
      case MakeReservation(guestId, startDate, endDate, roomNumber, replyTo) =>
        val tentativeReservation = Reservation.make(guestId, hotelId, startDate, endDate, roomNumber)
        val conflictingReservationOption = state.reservations.find(r => r.intersect(tentativeReservation))

        conflictingReservationOption match {
          case None => Effect           // success: persist event, can make reservation
            .persist(ReservationAccepted(tentativeReservation)) // persist
            .thenReply(replyTo)(s => ReservationAccepted(tentativeReservation)) // reply to the "manager"
          case Some(conflictReservation) => Effect.reply(replyTo)(CommandFailure(s"Reservation failed: conflict with another reservation: ${conflictReservation.confirmationNumber}"))
        }

      case ChangeReservation(confirmationNumber, startDate, endDate, roomNumber, replyTo) =>
        Effect.none //todos
      case CancelReservation(confirmationNumber, replyTo) =>
        Effect.none //todo
    }

  def eventHandler(hotelId: String): (State, Event) => State = (state, event) =>
    event match {
      case ReservationAccepted(res) =>
        val newState = state.copy(reservations = state.reservations + res)
        println(s"Reservation ACCEPTED. New State: $newState")
        newState
      case ReservationUpdated(oldReservation, newReservation) =>
        val newState = state.copy(reservations = state.reservations - oldReservation + newReservation)
        println(s"Reservation UPDATED. New State: $newState")
        newState
      case ReservationCanceled(res) =>
        val newState = state.copy(reservations = state.reservations - res)
        println(s"Reservation CANCELLED. New State: $newState")
        newState
    }

  def apply(hotelId: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId(hotelId),
      emptyState = State(Set.empty),
      commandHandler = commandHandler(hotelId),
      eventHandler = eventHandler(hotelId)
    )

}
