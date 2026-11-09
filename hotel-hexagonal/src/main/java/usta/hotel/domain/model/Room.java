package usta.hotel.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/** Clase que representa una habitación del hotel. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Room {

    private Long id;

    // Número visible de la habitación (por ejemplo "101")
    private String number;

    // Tipo: Sencilla, Doble, Suite, Familiar
    private String type;

    private String description;

    // Precio por noche
    private BigDecimal pricePerNight;

    // Máximo de huéspedes
    private int capacity;

    // Estado de la habitación (DISPONIBLE o EN MANTENIMIENTO)
    @Builder.Default
    private RoomStatus status = RoomStatus.AVAILABLE;
}
