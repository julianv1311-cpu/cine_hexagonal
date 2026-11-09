package usta.hotel.application.port.in;

import usta.hotel.domain.model.Room;

import java.math.BigDecimal;
import java.util.List;

// Puerto primario: operaciones para administrar las habitaciones
public interface ManageRoomsUseCase {

    Room create(String number, String type, String description, BigDecimal pricePerNight, int capacity);

    Room update(Long id, String number, String type, String description, BigDecimal pricePerNight, int capacity);

    Room toggleMaintenance(Long id);

    List<Room> findAll();

    Room findById(Long id);
}
