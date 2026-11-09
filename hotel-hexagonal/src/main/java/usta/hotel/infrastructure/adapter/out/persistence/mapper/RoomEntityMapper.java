package usta.hotel.infrastructure.adapter.out.persistence.mapper;

import usta.hotel.domain.model.Room;
import usta.hotel.infrastructure.adapter.out.persistence.entity.RoomEntity;

// Convierte entre la habitación del dominio (Room) y la entidad JPA (RoomEntity)
public final class RoomEntityMapper {

    private RoomEntityMapper() {
    }

    // Dominio -> entidad (para guardar)
    public static RoomEntity toEntity(Room room) {
        if (room == null) {
            return null;
        }
        RoomEntity entity = new RoomEntity();
        entity.setId(room.getId());
        entity.setNumber(room.getNumber());
        entity.setType(room.getType());
        entity.setDescription(room.getDescription());
        entity.setPricePerNight(room.getPricePerNight());
        entity.setCapacity(room.getCapacity());
        entity.setStatus(room.getStatus());
        return entity;
    }

    // Entidad -> dominio (para leer)
    public static Room toDomain(RoomEntity entity) {
        if (entity == null) {
            return null;
        }
        return Room.builder()
                .id(entity.getId())
                .number(entity.getNumber())
                .type(entity.getType())
                .description(entity.getDescription())
                .pricePerNight(entity.getPricePerNight())
                .capacity(entity.getCapacity())
                .status(entity.getStatus())
                .build();
    }
}
