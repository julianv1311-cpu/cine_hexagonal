package usta.hotel.infrastructure.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import usta.hotel.application.port.out.GuestRepository;
import usta.hotel.application.port.out.ReservationRepository;
import usta.hotel.application.port.out.RoomRepository;
import usta.hotel.domain.model.Guest;
import usta.hotel.domain.model.Reservation;
import usta.hotel.domain.model.ReservationStatus;
import usta.hotel.domain.model.Room;
import usta.hotel.domain.model.RoomStatus;
import usta.hotel.domain.service.PriceCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// Carga datos de prueba coherentes la primera vez que arranca la aplicación (tablas vacías).
// Usa los puertos de salida, así que no conoce los detalles de JPA.
@ApplicationScoped
public class DataSeeder {

    private static final Logger LOG = Logger.getLogger(DataSeeder.class);

    @Inject RoomRepository rooms;
    @Inject GuestRepository guests;
    @Inject ReservationRepository reservations;

    @ConfigProperty(name = "hotel.seed.enabled", defaultValue = "true")
    boolean enabled;

    private final PriceCalculator priceCalculator = new PriceCalculator();

    @Transactional
    void onStart(@Observes StartupEvent event) {
        if (!enabled || !rooms.findAll().isEmpty()) {
            return;
        }
        LOG.info("Cargando datos de prueba del hotel...");

        Room r101 = rooms.save(room("101", "Sencilla", "Cama semidoble con lencería blanca, mesa de noche y piso en cerámica", "180000", 1, RoomStatus.AVAILABLE));
        Room r102 = rooms.save(room("102", "Doble", "Dos camas dobles con cabecero en madera, lámparas de lectura y aire acondicionado", "260000", 2, RoomStatus.AVAILABLE));
        Room r201 = rooms.save(room("201", "Doble", "Dos camas dobles, escritorio, teléfono y aire acondicionado", "280000", 2, RoomStatus.AVAILABLE));
        Room r202 = rooms.save(room("202", "Familiar", "Tres camas dobles, jacuzzi privado y espacio amplio para toda la familia", "420000", 6, RoomStatus.AVAILABLE));
        rooms.save(room("203", "Sencilla", "Cama semidoble, mesa de noche y ambiente tranquilo", "170000", 1, RoomStatus.MAINTENANCE));
        Room r301 = rooms.save(room("301", "Suite Dúplex", "Suite dúplex con jacuzzi, escalera a segundo nivel, sala con TV e iluminación LED", "650000", 4, RoomStatus.AVAILABLE));
        rooms.save(room("302", "Suite Panorámica", "Suite con cama king, tina exenta junto al ventanal panorámico y terraza privada", "750000", 2, RoomStatus.AVAILABLE));

        Guest laura = guests.save(guest("1052345678", "Laura", "Gómez Rincón", "laura.gomez@correo.com", "3104567890", "Tunja", LocalDate.of(2026, 10, 31)));
        Guest carlos = guests.save(guest("80123456", "Carlos", "Pérez Díaz", "carlos.perez@correo.com", "3157894561", "Tunja", LocalDate.of(2026, 11, 1)));
        Guest andrea = guests.save(guest("1098765432", "Andrea", "Martínez López", "andrea.martinez@correo.com", "3201234567", "Tunja", LocalDate.of(2026, 11, 2)));
        Guest jorge = guests.save(guest("7178945", "Jorge", "Ramírez Suárez", "jorge.ramirez@correo.com", "3112223344", "Tunja", LocalDate.of(2026, 11, 4)));
        Guest sofia = guests.save(guest("1002003004", "Sofía", "Castro Vargas", "sofia.castro@correo.com", "3009876543", "Tunja", LocalDate.of(2026, 11, 5)));

        LocalDate today = LocalDate.now();
        reservations.save(reservation(laura, r102, today, today.plusDays(3), 2, ReservationStatus.CHECKED_IN));
        reservations.save(reservation(sofia, r101, today, today.plusDays(2), 1, ReservationStatus.CONFIRMED));
        reservations.save(reservation(carlos, r201, today.plusDays(2), today.plusDays(5), 2, ReservationStatus.CONFIRMED));
        reservations.save(reservation(andrea, r301, today.plusDays(7), today.plusDays(15), 3, ReservationStatus.CONFIRMED));
        reservations.save(reservation(laura, r202, today.plusDays(1), today.plusDays(4), 4, ReservationStatus.CONFIRMED));
        reservations.save(reservation(jorge, r101, today.minusDays(10), today.minusDays(7), 1, ReservationStatus.COMPLETED));
        reservations.save(reservation(sofia, r202, today.minusDays(3), today.minusDays(1), 3, ReservationStatus.COMPLETED));
        reservations.save(reservation(carlos, r101, today.plusDays(10), today.plusDays(12), 1, ReservationStatus.CANCELLED));

        LOG.info("Datos de prueba cargados: 7 habitaciones, 5 huéspedes y 8 reservas");
    }

    private Room room(String number, String type, String description, String price, int capacity,
                      RoomStatus status) {
        return Room.builder()
                .number(number)
                .type(type)
                .description(description)
                .pricePerNight(new BigDecimal(price))
                .capacity(capacity)
                .status(status)
                .build();
    }

    private Guest guest(String document, String firstName, String lastName, String email, String phone, String city,
                        LocalDate registeredAt) {
        return Guest.builder()
                .document(document)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone(phone)
                .city(city)
                .registeredAt(registeredAt)
                .build();
    }

    private Reservation reservation(Guest guest, Room room, LocalDate checkIn, LocalDate checkOut,
                                    int guestsCount, ReservationStatus status) {
        return Reservation.builder()
                .guest(guest)
                .room(room)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .guestsCount(guestsCount)
                .total(priceCalculator.total(room.getPricePerNight(), checkIn, checkOut))
                .status(status)
                .createdAt(LocalDateTime.of(checkIn.minusDays(5), LocalTime.of(10, 30)))
                .build();
    }
}
