package usta.hotel.infrastructure.adapter.out.persistence.mapper;

import usta.hotel.domain.model.Guest;
import usta.hotel.infrastructure.adapter.out.persistence.entity.GuestEntity;

// Convierte entre el huésped del dominio (Guest) y la entidad JPA (GuestEntity)
public final class GuestEntityMapper {

    private GuestEntityMapper() {
    }

    // Dominio -> entidad (para guardar)
    public static GuestEntity toEntity(Guest guest) {
        if (guest == null) {
            return null;
        }
        GuestEntity entity = new GuestEntity();
        entity.setId(guest.getId());
        entity.setDocument(guest.getDocument());
        entity.setFirstName(guest.getFirstName());
        entity.setLastName(guest.getLastName());
        entity.setEmail(guest.getEmail());
        entity.setPhone(guest.getPhone());
        entity.setCity(guest.getCity());
        entity.setRegisteredAt(guest.getRegisteredAt());
        return entity;
    }

    // Entidad -> dominio (para leer)
    public static Guest toDomain(GuestEntity entity) {
        if (entity == null) {
            return null;
        }
        return Guest.builder()
                .id(entity.getId())
                .document(entity.getDocument())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .city(entity.getCity())
                .registeredAt(entity.getRegisteredAt())
                .build();
    }
}
