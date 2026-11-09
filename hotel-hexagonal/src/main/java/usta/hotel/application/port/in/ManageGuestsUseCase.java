package usta.hotel.application.port.in;

import usta.hotel.domain.model.Guest;

import java.util.List;

// Puerto primario: operaciones para registrar y consultar huéspedes
public interface ManageGuestsUseCase {

    Guest register(String document, String firstName, String lastName, String email, String phone, String city);

    List<Guest> findAll();

    Guest findById(Long id);
}
