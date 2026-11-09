package usta.hotel.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/** Clase que representa un huésped (cliente) del hotel. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Guest {

    private Long id;

    // Número de documento (cédula o pasaporte)
    private String document;

    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String city;

    // Fecha en que se registró el huésped
    @Builder.Default
    private LocalDate registeredAt = LocalDate.now();

    // Nombre completo para mostrar
    public String fullName() {
        return firstName + " " + lastName;
    }
}
