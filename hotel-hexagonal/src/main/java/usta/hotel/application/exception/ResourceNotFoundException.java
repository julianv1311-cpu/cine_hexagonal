package usta.hotel.application.exception;

// Excepción que se lanza cuando un recurso no existe
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
