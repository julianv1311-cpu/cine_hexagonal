package usta.hotel.application.port.in;

import usta.hotel.domain.model.Reservation;

import java.time.LocalDate;
import java.util.List;

// Puerto primario: operaciones para reservar y cambiar el estado de las reservas
public interface ManageReservationsUseCase {

    Reservation book(Long guestId, Long roomId, LocalDate checkIn, LocalDate checkOut, int guestsCount);

    Reservation checkIn(Long reservationId);

    Reservation checkOut(Long reservationId);

    Reservation cancel(Long reservationId);

    List<Reservation> findAll();

    Reservation findById(Long reservationId);

    List<Reservation> findByCheckInBetween(LocalDate from, LocalDate to);

    boolean isRoomOccupiedOn(Long roomId, LocalDate date);
}
