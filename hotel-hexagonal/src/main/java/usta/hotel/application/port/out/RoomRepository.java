package usta.hotel.application.port.out;

import usta.hotel.domain.model.Room;

import java.util.List;
import java.util.Optional;

// Puerto secundario: persistencia de habitaciones
public interface RoomRepository {

    Room save(Room room);

    List<Room> findAll();

    Optional<Room> findById(Long id);
}
