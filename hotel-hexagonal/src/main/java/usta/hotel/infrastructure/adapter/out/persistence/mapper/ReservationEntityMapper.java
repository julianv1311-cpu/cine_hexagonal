package usta.hotel.infrastructure.adapter.out.persistence.mapper;

import usta.hotel.domain.model.Reservation;
import usta.hotel.infrastructure.adapter.out.persistence.entity.GuestEntity;
import usta.hotel.infrastructure.adapter.out.persistence.entity.ReservationEntity;
import usta.hotel.infrastructure.adapter.out.persistence.entity.RoomEntity;

// Convierte entre la reserva del dominio (Reservation) y la entidad JPA (ReservationEntity)
public final class ReservationEntityMapper {

    private ReservationEntityMapper() {
    }

    // Dominio -> entidad. Recibe el huésped y la habitación ya resueltos (relaciones).
    public static ReservationEntity toEntity(Reservation reservation, GuestEntity guestEntity, RoomEntity roomEntity) {
        if (reservation == null) {
            return null;
        }
        ReservationEntity entity = new ReservationEntity();
        entity.setId(reservation.getId());
        entity.setGuest(guestEntity);
        entity.setRoom(roomEntity);
        entity.setCheckIn(reservation.getCheckIn());
        entity.setCheckOut(reservation.getCheckOut());
        entity.setGuestsCount(reservation.getGuestsCount());
        entity.setTotal(reservation.getTotal());
        entity.setStatus(reservation.getStatus());
        entity.setCreatedAt(reservation.getCreatedAt());
        return entity;
    }

    // Entidad -> dominio
    public static Reservation toDomain(ReservationEntity entity) {
        if (entity == null) {
            return null;
        }
        return Reservation.builder()
                .id(entity.getId())
                .guest(GuestEntityMapper.toDomain(entity.getGuest()))
                .room(RoomEntityMapper.toDomain(entity.getRoom()))
                .checkIn(entity.getCheckIn())
                .checkOut(entity.getCheckOut())
                .guestsCount(entity.getGuestsCount())
                .total(entity.getTotal())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
