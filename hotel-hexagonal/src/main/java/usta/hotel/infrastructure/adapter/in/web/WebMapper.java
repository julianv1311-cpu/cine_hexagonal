package usta.hotel.infrastructure.adapter.in.web;

import usta.hotel.domain.exception.BusinessRuleException;
import usta.hotel.domain.model.Guest;
import usta.hotel.domain.model.Reservation;
import usta.hotel.domain.model.ReservationStatus;
import usta.hotel.domain.model.Room;
import usta.hotel.domain.model.RoomStatus;
import usta.hotel.domain.service.PriceCalculator;
import usta.hotel.domain.service.RoomPolicy;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Convierte datos del dominio a modelos listos para la vista web (y lee los datos de los formularios)
public final class WebMapper {

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Foto de cada tipo de habitación (META-INF/resources/img/rooms). La foto la define el tipo,
    // así la imagen siempre corresponde a la habitación (una suite se ve como suite).
    private static final Map<String, String> TYPE_PHOTOS = new LinkedHashMap<>();
    // Ícono de respaldo por tipo (si falta la foto se muestra el ícono)
    private static final Map<String, String> TYPE_ICONS = new LinkedHashMap<>();

    static {
        TYPE_PHOTOS.put("Sencilla", "sencilla.jpg");
        TYPE_PHOTOS.put("Doble", "doble.jpg");
        TYPE_PHOTOS.put("Familiar", "familiar.jpg");
        TYPE_PHOTOS.put("Suite Dúplex", "suite-duplex.jpg");
        TYPE_PHOTOS.put("Suite Panorámica", "suite-panoramica.jpg");
        TYPE_ICONS.put("Sencilla", "bi-person");
        TYPE_ICONS.put("Doble", "bi-people");
        TYPE_ICONS.put("Familiar", "bi-house-heart");
        TYPE_ICONS.put("Suite Dúplex", "bi-gem");
        TYPE_ICONS.put("Suite Panorámica", "bi-window-sidebar");
    }

    // Tipos de habitación que maneja el hotel
    public static final List<String> ROOM_TYPES = RoomPolicy.ROOM_TYPES;

    private static final PriceCalculator PRICE_CALCULATOR = new PriceCalculator();

    private WebMapper() {
    }

    // ---------------------------------------------------------------- Habitaciones

    // Datos de una habitación para el listado principal
    public static RoomView toView(Room room, boolean occupiedToday) {
        boolean maintenance = room.getStatus() == RoomStatus.MAINTENANCE;
        String label;
        String css;
        if (maintenance) {
            label = "En mantenimiento";
            css = "text-bg-warning";
        } else if (occupiedToday) {
            label = "Ocupada hoy";
            css = "text-bg-info";
        } else {
            label = "Disponible hoy";
            css = "text-bg-success";
        }
        return new RoomView(
                room.getId(),
                room.getNumber(),
                room.getType(),
                room.getDescription() == null ? "" : room.getDescription(),
                money(room.getPricePerNight()),
                room.getCapacity(),
                photoUrl(room.getType()),
                typeIcon(room.getType()),
                label,
                css,
                maintenance,
                occupiedToday);
    }

    // Ruta de la foto según el tipo de habitación; null si la imagen no existe en el proyecto
    public static String photoUrl(String type) {
        String file = type == null ? null : TYPE_PHOTOS.get(type);
        if (file == null || WebMapper.class.getClassLoader().getResource("META-INF/resources/img/rooms/" + file) == null) {
            return null;
        }
        return "/img/rooms/" + file;
    }

    // Ícono de Bootstrap Icons según el tipo de habitación
    public static String typeIcon(String type) {
        return TYPE_ICONS.getOrDefault(type, "bi-door-closed");
    }

    // Tarjetas del selector de tipo (foto + nombre + capacidad máxima)
    public static List<TypeOption> typeOptions(String selected) {
        List<TypeOption> options = new ArrayList<>();
        for (String type : ROOM_TYPES) {
            // Clave sin tildes ni espacios para el id del botón (suite-duplex, suite-panoramica)
            String key = Normalizer.normalize(type, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "").toLowerCase().replace(' ', '-');
            options.add(new TypeOption(key, type, photoUrl(type), typeIcon(type),
                    RoomPolicy.maxCapacity(type), type.equals(selected)));
        }
        return options;
    }

    // Formulario vacío para crear una habitación
    public static RoomForm emptyRoomForm() {
        return new RoomForm(null, "", "Sencilla", "", "", "1");
    }

    // Formulario con los datos de una habitación existente (para editar)
    public static RoomForm toForm(Room room) {
        return new RoomForm(room.getId(), room.getNumber(), room.getType(),
                room.getDescription() == null ? "" : room.getDescription(),
                room.getPricePerNight().stripTrailingZeros().toPlainString(),
                String.valueOf(room.getCapacity()));
    }

    // ---------------------------------------------------------------- Huéspedes

    public static GuestView toView(Guest guest) {
        return new GuestView(
                guest.getId(),
                guest.getDocument(),
                guest.fullName(),
                guest.getEmail(),
                guest.getPhone(),
                guest.getCity() == null ? "" : guest.getCity(),
                guest.getRegisteredAt() == null ? "" : DATE_FORMAT.format(guest.getRegisteredAt()));
    }

    public static GuestForm emptyGuestForm() {
        return new GuestForm("", "", "", "", "", "Tunja");
    }

    // ---------------------------------------------------------------- Reservas

    public static ReservationView toView(Reservation reservation) {
        ReservationStatus status = reservation.getStatus();
        long nights = PRICE_CALCULATOR.nights(reservation.getCheckIn(), reservation.getCheckOut());
        return new ReservationView(
                reservation.getId(),
                reservation.getGuest().fullName(),
                reservation.getGuest().getDocument(),
                reservation.getRoom().getId(),
                reservation.getRoom().getNumber(),
                reservation.getRoom().getType(),
                DATE_FORMAT.format(reservation.getCheckIn()),
                DATE_FORMAT.format(reservation.getCheckOut()),
                nights,
                reservation.getGuestsCount(),
                money(reservation.getTotal()),
                statusLabel(status),
                statusCss(status),
                status == ReservationStatus.CONFIRMED,
                status == ReservationStatus.CHECKED_IN,
                status == ReservationStatus.CONFIRMED,
                reservation.getCreatedAt() == null ? "" : DATE_TIME_FORMAT.format(reservation.getCreatedAt()));
    }

    // Desglose de la cuenta de una reserva (precio por noche, recargos y descuento)
    public static BillView toBill(Reservation reservation) {
        BigDecimal price = reservation.getRoom().getPricePerNight();
        long nights = PRICE_CALCULATOR.nights(reservation.getCheckIn(), reservation.getCheckOut());
        long weekendNights = PRICE_CALCULATOR.weekendNights(reservation.getCheckIn(), reservation.getCheckOut());
        BigDecimal subtotal = PRICE_CALCULATOR.subtotal(price, reservation.getCheckIn(), reservation.getCheckOut());
        BigDecimal discount = PRICE_CALCULATOR.discount(nights, subtotal);
        return new BillView(money(price), nights, weekendNights, money(subtotal), money(discount),
                discount.signum() > 0, money(reservation.getTotal()));
    }

    public static String statusLabel(ReservationStatus status) {
        return switch (status) {
            case CONFIRMED -> "Confirmada";
            case CHECKED_IN -> "Hospedado";
            case COMPLETED -> "Finalizada";
            case CANCELLED -> "Cancelada";
        };
    }

    public static String statusCss(ReservationStatus status) {
        return switch (status) {
            case CONFIRMED -> "text-bg-primary";
            case CHECKED_IN -> "text-bg-success";
            case COMPLETED -> "text-bg-secondary";
            case CANCELLED -> "text-bg-danger";
        };
    }

    // Opciones de huéspedes para el formulario de reserva
    public static List<OptionView> guestOptions(List<Guest> guests, String selectedId) {
        List<OptionView> options = new ArrayList<>();
        for (Guest guest : guests) {
            String value = String.valueOf(guest.getId());
            options.add(new OptionView(value, guest.fullName() + " - " + guest.getDocument(), value.equals(selectedId)));
        }
        return options;
    }

    // Opciones de habitaciones (solo las que no están en mantenimiento)
    public static List<OptionView> roomOptions(List<Room> rooms, String selectedId) {
        List<OptionView> options = new ArrayList<>();
        for (Room room : rooms) {
            if (room.getStatus() == RoomStatus.MAINTENANCE) {
                continue;
            }
            String value = String.valueOf(room.getId());
            String label = room.getNumber() + " - " + room.getType() + " ($" + money(room.getPricePerNight())
                    + " por noche, máx. " + room.getCapacity() + ")";
            options.add(new OptionView(value, label, value.equals(selectedId)));
        }
        return options;
    }

    // Opciones simples (tipos de habitación, fotos)
    public static List<OptionView> options(List<String> values, String selected) {
        List<OptionView> options = new ArrayList<>();
        for (String value : values) {
            options.add(new OptionView(value, value, value.equals(selected)));
        }
        return options;
    }

    // ---------------------------------------------------------------- Lectura de formularios

    public static BigDecimal parseMoney(String text, String field) {
        if (text == null || text.isBlank()) {
            throw new BusinessRuleException("El campo " + field + " es obligatorio");
        }
        // Se aceptan valores como 180000, 180.000 o 180000,50
        String clean = text.trim().replace("$", "").replace(" ", "");
        if (clean.contains(",")) {
            clean = clean.replace(".", "").replace(",", ".");
        } else if (clean.matches("\\d{1,3}(\\.\\d{3})+")) {
            clean = clean.replace(".", "");
        }
        try {
            return new BigDecimal(clean);
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("El campo " + field + " debe ser un número válido");
        }
    }

    public static int parseInt(String text, String field) {
        if (text == null || text.isBlank()) {
            throw new BusinessRuleException("El campo " + field + " es obligatorio");
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("El campo " + field + " debe ser un número entero");
        }
    }

    public static Long parseId(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Lee una fecha del formulario (formato del navegador: yyyy-MM-dd)
    public static LocalDate parseDate(String text, String field) {
        if (text == null || text.isBlank()) {
            throw new BusinessRuleException("La " + field + " es obligatoria");
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessRuleException("La " + field + " no tiene un formato válido");
        }
    }

    // Formato de dinero con punto de miles (180.000)
    public static String money(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return String.format(Locale.US, "%,.0f", value).replace(',', '.');
    }

    public static String safe(String value) {
        return value == null ? "" : value;
    }

    // ---------------------------------------------------------------- Modelos de la vista

    public record RoomView(Long id, String number, String type, String description, String price,
                           int capacity, String photoUrl, String typeIcon, String statusLabel, String statusCss,
                           boolean maintenance, boolean occupiedToday) {
    }

    public record RoomForm(Long id, String number, String type, String description, String price,
                           String capacity) {
    }

    public record GuestView(Long id, String document, String fullName, String email, String phone,
                            String city, String registeredAt) {
    }

    public record GuestForm(String document, String firstName, String lastName, String email,
                            String phone, String city) {
    }

    public record ReservationView(Long id, String guestName, String guestDocument, Long roomId,
                                  String roomNumber, String roomType, String checkIn, String checkOut,
                                  long nights, int guestsCount, String total, String statusLabel,
                                  String statusCss, boolean canCheckIn, boolean canCheckOut,
                                  boolean canCancel, String createdAt) {
    }

    public record ReservationForm(String guestId, String roomId, String checkIn, String checkOut,
                                  String guestsCount) {
    }

    public record BillView(String pricePerNight, long nights, long weekendNights, String subtotal,
                           String discount, boolean hasDiscount, String total) {
    }

    public record OptionView(String value, String label, boolean selected) {
    }

    public record TypeOption(String key, String value, String photoUrl, String icon, int maxCapacity,
                             boolean selected) {
    }

    public record StatusFilter(String value, String label, boolean active, long count) {
    }
}
