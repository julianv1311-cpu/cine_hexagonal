package usta.hotel.application.port.out;

import usta.hotel.domain.model.Guest;

import java.util.List;
import java.util.Optional;

// Puerto secundario: persistencia de huéspedes
public interface GuestRepository {

    Guest save(Guest guest);

    List<Guest> findAll();

    Optional<Guest> findById(Long id);
}
