package usta.hotel.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Clase que representa la reserva de una habitación por parte de un huésped. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Reservation {

    private Long id;
    private Guest guest;
    private Room room;

    // Fecha de entrada y de salida
    private LocalDate checkIn;
    private LocalDate checkOut;

    // Cantidad de personas que se hospedan
    private int guestsCount;

    // Valor total de la estadía
    private BigDecimal total;

    // Estado de la reserva
    @Builder.Default
    private ReservationStatus status = ReservationStatus.CONFIRMED;

    // Momento en que se registró la reserva
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
