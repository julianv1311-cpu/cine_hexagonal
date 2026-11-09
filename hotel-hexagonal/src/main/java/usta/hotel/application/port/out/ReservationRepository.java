package usta.hotel.application.port.out;

import usta.hotel.domain.model.Reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Puerto secundario: persistencia de reservas
public interface ReservationRepository {

    Reservation save(Reservation reservation);

    List<Reservation> findAll();

    Optional<Reservation> findById(Long id);

    List<Reservation> findByRoomId(Long roomId);

    List<Reservation> findByCheckInBetween(LocalDate from, LocalDate to);
}
