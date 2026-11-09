package usta.hotel.domain.exception;

// Excepción que se lanza cuando se incumple una regla de negocio
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
