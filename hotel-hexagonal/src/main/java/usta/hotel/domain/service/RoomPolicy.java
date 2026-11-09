package usta.hotel.domain.service;

import usta.hotel.domain.exception.BusinessRuleException;
import usta.hotel.domain.model.Reservation;
import usta.hotel.domain.model.ReservationStatus;
import usta.hotel.domain.model.Room;
import usta.hotel.domain.model.RoomStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// Servicio con las reglas de negocio de las habitaciones
public class RoomPolicy {

    // Tipos de habitación del hotel y capacidad máxima de cada uno
    public static final List<String> ROOM_TYPES = List.of("Sencilla", "Doble", "Familiar", "Suite Dúplex", "Suite Panorámica");
    private static final Map<String, Integer> MAX_CAPACITY_BY_TYPE = Map.of(
            "Sencilla", 1,
            "Doble", 2,
            "Familiar", 6,
            "Suite Dúplex", 4,
            "Suite Panorámica", 2);

    // Capacidad máxima permitida para un tipo de habitación
    public static int maxCapacity(String type) {
        return MAX_CAPACITY_BY_TYPE.getOrDefault(type, 0);
    }

    // Valida los datos de una habitación antes de guardarla
    public void validate(Room room, List<Room> existing) {
        if (room.getNumber() == null || room.getNumber().isBlank()) {
            throw new BusinessRuleException("El número de la habitación es obligatorio");
        }
        if (!room.getNumber().trim().matches("\\d{2,4}")) {
            throw new BusinessRuleException("El número de la habitación debe tener entre 2 y 4 dígitos (por ejemplo 101)");
        }
        if (room.getType() == null || !ROOM_TYPES.contains(room.getType())) {
            throw new BusinessRuleException("El tipo de habitación debe ser Sencilla, Doble, Familiar, Suite Dúplex o Suite Panorámica");
        }
        if (room.getPricePerNight() == null || room.getPricePerNight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("El precio por noche debe ser mayor a 0");
        }
        int max = maxCapacity(room.getType());
        if (room.getCapacity() <= 0 || room.getCapacity() > max) {
            throw new BusinessRuleException("Una habitación " + room.getType() + " admite entre 1 y " + max + " huéspedes");
        }
        if (existing != null) {
            for (Room other : existing) {
                boolean sameNumber = other.getNumber().equalsIgnoreCase(room.getNumber().trim());
                boolean sameRoom = room.getId() != null && room.getId().equals(other.getId());
                if (sameNumber && !sameRoom) {
                    throw new BusinessRuleException("Ya existe la habitación número " + room.getNumber());
                }
            }
        }
    }

    // Cambia el estado de la habitación (disponible <-> mantenimiento)
    public Room toggleMaintenance(Room room, List<Reservation> roomReservations) {
        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            room.setStatus(RoomStatus.AVAILABLE);
            return room;
        }
        if (roomReservations != null) {
            for (Reservation reservation : roomReservations) {
                if (reservation.getStatus() == ReservationStatus.CHECKED_IN) {
                    throw new BusinessRuleException("La habitación " + room.getNumber()
                            + " tiene un huésped hospedado; no se puede pasar a mantenimiento");
                }
            }
        }
        room.setStatus(RoomStatus.MAINTENANCE);
        return room;
    }
}
