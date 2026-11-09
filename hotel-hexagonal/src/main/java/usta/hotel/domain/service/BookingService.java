package usta.hotel.domain.service;

import usta.hotel.domain.exception.BusinessRuleException;
import usta.hotel.domain.model.Guest;
import usta.hotel.domain.model.Reservation;
import usta.hotel.domain.model.ReservationStatus;
import usta.hotel.domain.model.Room;
import usta.hotel.domain.model.RoomStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

// Servicio que aplica las reglas de negocio de las reservas del hotel
public class BookingService {

    // Estadía máxima permitida en una sola reserva
    public static final int MAX_NIGHTS = 30;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PriceCalculator priceCalculator = new PriceCalculator();

    // Crea una reserva validando fechas, capacidad, estado de la habitación y cruces
    public Reservation book(Guest guest, Room room, LocalDate checkIn, LocalDate checkOut,
                            int guestsCount, List<Reservation> roomReservations, LocalDate today) {
        if (guest == null) {
            throw new BusinessRuleException("Debe seleccionar el huésped");
        }
        if (room == null) {
            throw new BusinessRuleException("Debe seleccionar la habitación");
        }
        if (checkIn == null || checkOut == null) {
            throw new BusinessRuleException("Las fechas de entrada y salida son obligatorias");
        }
        if (checkIn.isBefore(today)) {
            throw new BusinessRuleException("La fecha de entrada no puede ser anterior a hoy");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BusinessRuleException("La fecha de salida debe ser posterior a la fecha de entrada");
        }
        long nights = priceCalculator.nights(checkIn, checkOut);
        if (nights > MAX_NIGHTS) {
            throw new BusinessRuleException("La estadía máxima es de " + MAX_NIGHTS + " noches");
        }
        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new BusinessRuleException("La habitación " + room.getNumber() + " está en mantenimiento");
        }
        if (guestsCount <= 0) {
            throw new BusinessRuleException("La cantidad de huéspedes debe ser mayor a 0");
        }
        if (guestsCount > room.getCapacity()) {
            throw new BusinessRuleException("La habitación " + room.getNumber()
                    + " admite máximo " + room.getCapacity() + " huéspedes");
        }
        validateNoOverlap(room, checkIn, checkOut, roomReservations);

        return Reservation.builder()
                .guest(guest)
                .room(room)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .guestsCount(guestsCount)
                .total(priceCalculator.total(room.getPricePerNight(), checkIn, checkOut))
                .build();
    }

    // Registra la llegada del huésped (check-in)
    public Reservation checkIn(Reservation reservation, LocalDate today) {
        requireReservation(reservation);
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessRuleException("Solo se puede hacer check-in a una reserva confirmada");
        }
        if (today.isBefore(reservation.getCheckIn())) {
            throw new BusinessRuleException("Aún no es la fecha de entrada de la reserva #" + reservation.getId());
        }
        if (!today.isBefore(reservation.getCheckOut())) {
            throw new BusinessRuleException("La fecha de la reserva #" + reservation.getId() + " ya pasó");
        }
        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservation;
    }

    // Registra la salida del huésped (check-out)
    public Reservation checkOut(Reservation reservation) {
        requireReservation(reservation);
        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new BusinessRuleException("Solo se puede hacer check-out a una reserva con el huésped hospedado");
        }
        reservation.setStatus(ReservationStatus.COMPLETED);
        return reservation;
    }

    // Cancela una reserva (solo si aún no ha llegado el huésped)
    public Reservation cancel(Reservation reservation) {
        requireReservation(reservation);
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessRuleException("La reserva ya estaba cancelada");
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessRuleException("No se puede cancelar una reserva con el huésped hospedado o finalizada");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        return reservation;
    }

    // Indica si la habitación está ocupada (reserva activa) en una fecha
    public boolean isOccupiedOn(List<Reservation> roomReservations, LocalDate date) {
        if (roomReservations == null) {
            return false;
        }
        for (Reservation reservation : roomReservations) {
            if (reservation.getStatus().isActive()
                    && !date.isBefore(reservation.getCheckIn())
                    && date.isBefore(reservation.getCheckOut())) {
                return true;
            }
        }
        return false;
    }

    // Valida que la habitación no tenga otra reserva activa que se cruce con las fechas
    private void validateNoOverlap(Room room, LocalDate checkIn, LocalDate checkOut, List<Reservation> roomReservations) {
        if (roomReservations == null) {
            return;
        }
        for (Reservation other : roomReservations) {
            boolean overlaps = other.getStatus().isActive()
                    && checkIn.isBefore(other.getCheckOut())
                    && other.getCheckIn().isBefore(checkOut);
            if (overlaps) {
                throw new BusinessRuleException("La habitación " + room.getNumber()
                        + " ya está reservada del " + DATE_FORMAT.format(other.getCheckIn())
                        + " al " + DATE_FORMAT.format(other.getCheckOut()) + " (reserva #" + other.getId() + ")");
            }
        }
    }

    private void requireReservation(Reservation reservation) {
        if (reservation == null) {
            throw new BusinessRuleException("La reserva no existe");
        }
    }
}
