package usta.hotel.domain.model;

// Enum con los estados de una reserva:
// CONFIRMED (confirmada) -> CHECKED_IN (hospedado) -> COMPLETED (finalizada)
// CONFIRMED -> CANCELLED (cancelada)
public enum ReservationStatus {
    CONFIRMED,
    CHECKED_IN,
    COMPLETED,
    CANCELLED;

    // Una reserva activa ocupa la habitación en sus fechas
    public boolean isActive() {
        return this == CONFIRMED || this == CHECKED_IN;
    }
}
