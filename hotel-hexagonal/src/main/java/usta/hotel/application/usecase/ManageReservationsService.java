package usta.hotel.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import usta.hotel.application.exception.ResourceNotFoundException;
import usta.hotel.application.port.in.ManageGuestsUseCase;
import usta.hotel.application.port.in.ManageReservationsUseCase;
import usta.hotel.application.port.in.ManageRoomsUseCase;
import usta.hotel.application.port.out.ReservationRepository;
import usta.hotel.domain.exception.BusinessRuleException;
import usta.hotel.domain.model.Guest;
import usta.hotel.domain.model.Reservation;
import usta.hotel.domain.model.Room;
import usta.hotel.domain.service.BookingService;

import java.time.LocalDate;
import java.util.List;

// Caso de uso: crea reservas y cambia su estado (check-in, check-out, cancelación)
@ApplicationScoped
public class ManageReservationsService implements ManageReservationsUseCase {

    private final ReservationRepository reservationRepository;
    private final ManageRoomsUseCase rooms;
    private final ManageGuestsUseCase guests;
    private final BookingService bookingService = new BookingService();

    @Inject
    public ManageReservationsService(ReservationRepository reservationRepository,
                                     ManageRoomsUseCase rooms,
                                     ManageGuestsUseCase guests) {
        this.reservationRepository = reservationRepository;
        this.rooms = rooms;
        this.guests = guests;
    }

    // Crea una reserva: el dominio valida fechas, capacidad, mantenimiento y cruces
    @Override
    public Reservation book(Long guestId, Long roomId, LocalDate checkIn, LocalDate checkOut, int guestsCount) {
        if (guestId == null) {
            throw new BusinessRuleException("Debe seleccionar el huésped");
        }
        if (roomId == null) {
            throw new BusinessRuleException("Debe seleccionar la habitación");
        }
        Guest guest = guests.findById(guestId);
        Room room = rooms.findById(roomId);
        Reservation reservation = bookingService.book(guest, room, checkIn, checkOut, guestsCount,
                reservationRepository.findByRoomId(roomId), LocalDate.now());
        return reservationRepository.save(reservation);
    }

    // Registra la llegada del huésped
    @Override
    public Reservation checkIn(Long reservationId) {
        Reservation reservation = findById(reservationId);
        bookingService.checkIn(reservation, LocalDate.now());
        return reservationRepository.save(reservation);
    }

    // Registra la salida del huésped
    @Override
    public Reservation checkOut(Long reservationId) {
        Reservation reservation = findById(reservationId);
        bookingService.checkOut(reservation);
        return reservationRepository.save(reservation);
    }

    // Cancela la reserva
    @Override
    public Reservation cancel(Long reservationId) {
        Reservation reservation = findById(reservationId);
        bookingService.cancel(reservation);
        return reservationRepository.save(reservation);
    }

    // Lista todas las reservas
    @Override
    public List<Reservation> findAll() {
        return reservationRepository.findAll();
    }

    // Busca una reserva por su id
    @Override
    public Reservation findById(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la reserva con id " + reservationId));
    }

    // Reservas cuya fecha de entrada está dentro del rango (para el reporte)
    @Override
    public List<Reservation> findByCheckInBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessRuleException("Debe indicar la fecha inicial y la fecha final del reporte");
        }
        if (to.isBefore(from)) {
            throw new BusinessRuleException("La fecha final del reporte no puede ser anterior a la inicial");
        }
        return reservationRepository.findByCheckInBetween(from, to);
    }

    // Indica si una habitación tiene una reserva activa en la fecha dada
    @Override
    public boolean isRoomOccupiedOn(Long roomId, LocalDate date) {
        return bookingService.isOccupiedOn(reservationRepository.findByRoomId(roomId), date);
    }
}
