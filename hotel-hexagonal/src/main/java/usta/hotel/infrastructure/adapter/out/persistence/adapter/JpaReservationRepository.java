package usta.hotel.infrastructure.adapter.out.persistence.adapter;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.hotel.application.port.out.ReservationRepository;
import usta.hotel.domain.model.Reservation;
import usta.hotel.infrastructure.adapter.out.persistence.entity.GuestEntity;
import usta.hotel.infrastructure.adapter.out.persistence.entity.ReservationEntity;
import usta.hotel.infrastructure.adapter.out.persistence.entity.RoomEntity;
import usta.hotel.infrastructure.adapter.out.persistence.mapper.ReservationEntityMapper;
import usta.hotel.infrastructure.adapter.out.persistence.repository.GuestJpaRepository;
import usta.hotel.infrastructure.adapter.out.persistence.repository.ReservationJpaRepository;
import usta.hotel.infrastructure.adapter.out.persistence.repository.RoomJpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Adaptador JPA del puerto ReservationRepository: guarda/lee reservas en MySQL
@ApplicationScoped
public class JpaReservationRepository implements ReservationRepository {

    @Inject
    ReservationJpaRepository repository;

    @Inject
    GuestJpaRepository guestRepository;

    @Inject
    RoomJpaRepository roomRepository;

    // Guarda la reserva resolviendo el huésped y la habitación por su id
    @Override
    @Transactional
    public Reservation save(Reservation reservation) {
        GuestEntity guestEntity = guestRepository.findByIdOptional(reservation.getGuest().getId())
                .orElseThrow(() -> new IllegalStateException("No existe el huésped con id " + reservation.getGuest().getId()));
        RoomEntity roomEntity = roomRepository.findByIdOptional(reservation.getRoom().getId())
                .orElseThrow(() -> new IllegalStateException("No existe la habitación con id " + reservation.getRoom().getId()));
        ReservationEntity saved = repository.getEntityManager()
                .merge(ReservationEntityMapper.toEntity(reservation, guestEntity, roomEntity));
        return ReservationEntityMapper.toDomain(saved);
    }

    // Devuelve todas las reservas (las más recientes primero)
    @Override
    public List<Reservation> findAll() {
        return repository.listAll(Sort.descending("checkIn").and("id", Sort.Direction.Descending)).stream()
                .map(ReservationEntityMapper::toDomain).toList();
    }

    // Busca una reserva por id
    @Override
    public Optional<Reservation> findById(Long id) {
        return repository.findByIdOptional(id).map(ReservationEntityMapper::toDomain);
    }

    // Reservas de una habitación
    @Override
    public List<Reservation> findByRoomId(Long roomId) {
        return repository.list("room.id", Sort.by("checkIn"), roomId).stream()
                .map(ReservationEntityMapper::toDomain).toList();
    }

    // Reservas cuya fecha de entrada está entre "from" y "to" (incluidas)
    @Override
    public List<Reservation> findByCheckInBetween(LocalDate from, LocalDate to) {
        return repository.list("checkIn >= ?1 and checkIn <= ?2", Sort.by("checkIn"), from, to).stream()
                .map(ReservationEntityMapper::toDomain).toList();
    }
}
