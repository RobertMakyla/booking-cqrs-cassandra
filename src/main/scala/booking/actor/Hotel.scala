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
        // if no reservation => failure
        // create new tentative reservation
        // find if they conflict, if so => failure
        // otherwise, persist ReservationChanged
        val oldReservationOption = state.reservations.find(_.confirmationNumber == confirmationNumber)
        val newReservationOption = oldReservationOption
          .map(res => res.copy(startDate = startDate, endDate = endDate, roomNumber = roomNumber))

        val reservationUpdatedEventOption = for {
          oldReservation <- oldReservationOption
          newReservation <- newReservationOption
        } yield ReservationUpdated(oldReservation, newReservation)

        val conflictingReservationOption = newReservationOption.flatMap { tentativeReservation =>
          state.reservations.find(r => r.confirmationNumber != confirmationNumber && r.intersect(tentativeReservation))
        }

        (reservationUpdatedEventOption, conflictingReservationOption) match {
          case (None, _) =>
            Effect.reply(replyTo)(CommandFailure(s"Cannot update reservation $confirmationNumber: not found"))
          case (_, Some(conflictRes)) =>
            Effect.reply(replyTo)(CommandFailure(s"Cannot update reservation $confirmationNumber: conflict with another reservation: ${conflictRes.confirmationNumber}"))
          case (Some(resUpdated), None) => // happy
            Effect.persist(resUpdated).thenReply(replyTo)(s => resUpdated)
        }
      case CancelReservation(confirmationNumber, replyTo) =>
        val reservationOption = state.reservations.find(_.confirmationNumber == confirmationNumber)
        reservationOption match {
          case Some(res) =>
            // success, confirmation found
            Effect.persist(ReservationCanceled(res)).thenReply(replyTo)(s => ReservationCanceled(res))
          case None =>
            // failure, reservation not found
            Effect.reply(replyTo)(CommandFailure(s"Cannot cancel reservation $confirmationNumber: not found"))
        }
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
