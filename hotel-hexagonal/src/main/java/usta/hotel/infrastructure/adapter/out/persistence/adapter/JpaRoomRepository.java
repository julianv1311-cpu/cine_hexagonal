package usta.hotel.infrastructure.adapter.out.persistence.adapter;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.hotel.application.port.out.RoomRepository;
import usta.hotel.domain.model.Room;
import usta.hotel.infrastructure.adapter.out.persistence.entity.RoomEntity;
import usta.hotel.infrastructure.adapter.out.persistence.mapper.RoomEntityMapper;
import usta.hotel.infrastructure.adapter.out.persistence.repository.RoomJpaRepository;

import java.util.List;
import java.util.Optional;

// Adaptador JPA del puerto RoomRepository: guarda/lee habitaciones en MySQL
@ApplicationScoped
public class JpaRoomRepository implements RoomRepository {

    @Inject
    RoomJpaRepository repository;

    // Guarda la habitación (inserta o actualiza) y la devuelve
    @Override
    @Transactional
    public Room save(Room room) {
        RoomEntity saved = repository.getEntityManager().merge(RoomEntityMapper.toEntity(room));
        return RoomEntityMapper.toDomain(saved);
    }

    // Devuelve todas las habitaciones ordenadas por número
    @Override
    public List<Room> findAll() {
        return repository.listAll(Sort.by("number")).stream().map(RoomEntityMapper::toDomain).toList();
    }

    // Busca una habitación por id
    @Override
    public Optional<Room> findById(Long id) {
        return repository.findByIdOptional(id).map(RoomEntityMapper::toDomain);
    }
}
