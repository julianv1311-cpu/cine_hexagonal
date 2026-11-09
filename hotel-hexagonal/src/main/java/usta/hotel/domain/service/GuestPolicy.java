package usta.hotel.domain.service;

import usta.hotel.domain.exception.BusinessRuleException;
import usta.hotel.domain.model.Guest;

import java.util.List;

// Servicio con las reglas de negocio de los huéspedes
public class GuestPolicy {

    private static final String DOCUMENT_PATTERN = "\\d{5,15}";
    private static final String EMAIL_PATTERN = "^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$";
    private static final String PHONE_PATTERN = "\\d{7,15}";

    // Valida los datos del huésped y que el documento no esté repetido
    public void validate(Guest guest, List<Guest> existing) {
        if (guest.getDocument() == null || !guest.getDocument().trim().matches(DOCUMENT_PATTERN)) {
            throw new BusinessRuleException("El documento debe tener entre 5 y 15 dígitos");
        }
        if (isBlank(guest.getFirstName()) || isBlank(guest.getLastName())) {
            throw new BusinessRuleException("El nombre y el apellido son obligatorios");
        }
        if (guest.getEmail() == null || !guest.getEmail().trim().matches(EMAIL_PATTERN)) {
            throw new BusinessRuleException("El correo electrónico no es válido");
        }
        if (guest.getPhone() == null || !guest.getPhone().trim().matches(PHONE_PATTERN)) {
            throw new BusinessRuleException("El teléfono debe tener entre 7 y 15 dígitos");
        }
        if (existing != null) {
            for (Guest other : existing) {
                boolean sameRecord = guest.getId() != null && guest.getId().equals(other.getId());
                if (!sameRecord && other.getDocument().equals(guest.getDocument().trim())) {
                    throw new BusinessRuleException("Ya existe un huésped con el documento " + guest.getDocument());
                }
                if (!sameRecord && other.getEmail().equalsIgnoreCase(guest.getEmail().trim())) {
                    throw new BusinessRuleException("Ya existe un huésped con el correo " + guest.getEmail());
                }
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
