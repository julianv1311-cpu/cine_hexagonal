package usta.hotel.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// Servicio que calcula el valor de una estadía aplicando recargos y descuentos
public class PriceCalculator {

    // Recargo del 15% para las noches de viernes y sábado
    private static final BigDecimal WEEKEND_SURCHARGE = new BigDecimal("0.15");
    // Descuento del 10% para estadías largas (7 noches o más)
    private static final BigDecimal LONG_STAY_DISCOUNT = new BigDecimal("0.10");
    public static final int LONG_STAY_NIGHTS = 7;

    // Cantidad de noches entre la entrada y la salida
    public long nights(LocalDate checkIn, LocalDate checkOut) {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    // Precio de una noche concreta (con recargo si es viernes o sábado)
    public BigDecimal nightlyRate(BigDecimal pricePerNight, LocalDate night) {
        BigDecimal price = pricePerNight;
        DayOfWeek day = night.getDayOfWeek();
        if (day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY) {
            price = price.add(pricePerNight.multiply(WEEKEND_SURCHARGE));
        }
        return price.setScale(0, RoundingMode.HALF_UP);
    }

    // Suma el precio de cada noche de la estadía
    public BigDecimal subtotal(BigDecimal pricePerNight, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightlyRate(pricePerNight, night));
        }
        return subtotal;
    }

    // Cantidad de noches que tienen recargo de fin de semana
    public long weekendNights(LocalDate checkIn, LocalDate checkOut) {
        long count = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            DayOfWeek day = night.getDayOfWeek();
            if (day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY) {
                count++;
            }
        }
        return count;
    }

    // Descuento por estadía larga
    public BigDecimal discount(long nights, BigDecimal subtotal) {
        if (nights >= LONG_STAY_NIGHTS) {
            return subtotal.multiply(LONG_STAY_DISCOUNT).setScale(0, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    // Total a pagar: subtotal menos el descuento
    public BigDecimal total(BigDecimal pricePerNight, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = subtotal(pricePerNight, checkIn, checkOut);
        return subtotal.subtract(discount(nights(checkIn, checkOut), subtotal));
    }
}
