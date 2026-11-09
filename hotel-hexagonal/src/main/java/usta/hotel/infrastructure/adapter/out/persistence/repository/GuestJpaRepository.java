package usta.hotel.infrastructure.adapter.out.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import usta.hotel.infrastructure.adapter.out.persistence.entity.GuestEntity;

// Repositorio Panache para la tabla guests
@ApplicationScoped
public class GuestJpaRepository implements PanacheRepository<GuestEntity> {
}
