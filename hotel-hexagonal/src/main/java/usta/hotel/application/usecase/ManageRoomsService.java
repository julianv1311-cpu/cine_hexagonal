package usta.hotel.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import usta.hotel.application.exception.ResourceNotFoundException;
import usta.hotel.application.port.in.ManageRoomsUseCase;
import usta.hotel.application.port.out.ReservationRepository;
import usta.hotel.application.port.out.RoomRepository;
import usta.hotel.domain.model.Room;
import usta.hotel.domain.service.RoomPolicy;

import java.math.BigDecimal;
import java.util.List;

// Caso de uso: administra las habitaciones del hotel
@ApplicationScoped
public class ManageRoomsService implements ManageRoomsUseCase {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final RoomPolicy roomPolicy = new RoomPolicy();

    @Inject
    public ManageRoomsService(RoomRepository roomRepository, ReservationRepository reservationRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
    }

    // Crea una habitación nueva (queda disponible)
    @Override
    public Room create(String number, String type, String description, BigDecimal pricePerNight,
                       int capacity) {
        Room room = Room.builder()
                .number(trim(number))
                .type(trim(type))
                .description(trim(description))
                .pricePerNight(pricePerNight)
                .capacity(capacity)
                .build();
        roomPolicy.validate(room, roomRepository.findAll());
        return roomRepository.save(room);
    }

    // Actualiza los datos de una habitación existente
    @Override
    public Room update(Long id, String number, String type, String description, BigDecimal pricePerNight,
                       int capacity) {
        Room room = findById(id);
        room.setNumber(trim(number));
        room.setType(trim(type));
        room.setDescription(trim(description));
        room.setPricePerNight(pricePerNight);
        room.setCapacity(capacity);
        roomPolicy.validate(room, roomRepository.findAll());
        return roomRepository.save(room);
    }

    // Cambia el estado: disponible <-> en mantenimiento
    @Override
    public Room toggleMaintenance(Long id) {
        Room room = findById(id);
        roomPolicy.toggleMaintenance(room, reservationRepository.findByRoomId(id));
        return roomRepository.save(room);
    }

    // Lista todas las habitaciones
    @Override
    public List<Room> findAll() {
        return roomRepository.findAll();
    }

    // Busca una habitación por su id
    @Override
    public Room findById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la habitación con id " + id));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
