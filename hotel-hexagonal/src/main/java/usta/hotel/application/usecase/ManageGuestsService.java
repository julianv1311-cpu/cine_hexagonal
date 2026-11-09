package usta.hotel.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import usta.hotel.application.exception.ResourceNotFoundException;
import usta.hotel.application.port.in.ManageGuestsUseCase;
import usta.hotel.application.port.out.GuestRepository;
import usta.hotel.domain.model.Guest;
import usta.hotel.domain.service.GuestPolicy;

import java.util.List;

// Caso de uso: registra y consulta los huéspedes
@ApplicationScoped
public class ManageGuestsService implements ManageGuestsUseCase {

    private final GuestRepository guestRepository;
    private final GuestPolicy guestPolicy = new GuestPolicy();

    @Inject
    public ManageGuestsService(GuestRepository guestRepository) {
        this.guestRepository = guestRepository;
    }

    // Registra un huésped nuevo validando sus datos
    @Override
    public Guest register(String document, String firstName, String lastName, String email,
                          String phone, String city) {
        Guest guest = Guest.builder()
                .document(trim(document))
                .firstName(trim(firstName))
                .lastName(trim(lastName))
                .email(email == null ? null : email.trim().toLowerCase())
                .phone(trim(phone))
                .city(trim(city))
                .build();
        guestPolicy.validate(guest, guestRepository.findAll());
        return guestRepository.save(guest);
    }

    // Lista todos los huéspedes
    @Override
    public List<Guest> findAll() {
        return guestRepository.findAll();
    }

    // Busca un huésped por su id
    @Override
    public Guest findById(Long id) {
        return guestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el huésped con id " + id));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
