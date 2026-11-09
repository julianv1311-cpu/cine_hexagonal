package usta.hotel.infrastructure.adapter.out.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import usta.hotel.infrastructure.adapter.out.persistence.entity.ReservationEntity;

// Repositorio Panache para la tabla reservations
@ApplicationScoped
public class ReservationJpaRepository implements PanacheRepository<ReservationEntity> {
}
