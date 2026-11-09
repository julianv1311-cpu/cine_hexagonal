package usta.hotel.infrastructure.adapter.out.persistence.adapter;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.hotel.application.port.out.GuestRepository;
import usta.hotel.domain.model.Guest;
import usta.hotel.infrastructure.adapter.out.persistence.entity.GuestEntity;
import usta.hotel.infrastructure.adapter.out.persistence.mapper.GuestEntityMapper;
import usta.hotel.infrastructure.adapter.out.persistence.repository.GuestJpaRepository;

import java.util.List;
import java.util.Optional;

// Adaptador JPA del puerto GuestRepository: guarda/lee huéspedes en MySQL
@ApplicationScoped
public class JpaGuestRepository implements GuestRepository {

    @Inject
    GuestJpaRepository repository;

    // Guarda el huésped (inserta o actualiza) y lo devuelve
    @Override
    @Transactional
    public Guest save(Guest guest) {
        GuestEntity saved = repository.getEntityManager().merge(GuestEntityMapper.toEntity(guest));
        return GuestEntityMapper.toDomain(saved);
    }

    // Devuelve todos los huéspedes ordenados por apellido
    @Override
    public List<Guest> findAll() {
        return repository.listAll(Sort.by("lastName").and("firstName")).stream()
                .map(GuestEntityMapper::toDomain).toList();
    }

    // Busca un huésped por id
    @Override
    public Optional<Guest> findById(Long id) {
        return repository.findByIdOptional(id).map(GuestEntityMapper::toDomain);
    }
}
