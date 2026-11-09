package usta.hotel.infrastructure.adapter.out.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import usta.hotel.infrastructure.adapter.out.persistence.entity.RoomEntity;

// Repositorio Panache para la tabla rooms
@ApplicationScoped
public class RoomJpaRepository implements PanacheRepository<RoomEntity> {
}
