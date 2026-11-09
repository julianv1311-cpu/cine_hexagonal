package usta.hotel.infrastructure.adapter.in.web;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import usta.hotel.application.port.in.ManageReservationsUseCase;
import usta.hotel.application.port.in.ManageRoomsUseCase;
import usta.hotel.domain.exception.BusinessRuleException;
import usta.hotel.domain.model.Reservation;
import usta.hotel.domain.model.ReservationStatus;
import usta.hotel.domain.model.Room;
import usta.hotel.domain.model.RoomStatus;
import usta.hotel.infrastructure.adapter.in.web.WebMapper.RoomForm;
import usta.hotel.infrastructure.adapter.in.web.WebMapper.RoomView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Adaptador de entrada WEB: página principal (listado de habitaciones) y formulario de habitaciones.
// Solo traduce las peticiones del navegador a llamadas a los puertos de entrada.
@Path("/")
@ApplicationScoped
@Tag(name = "Habitaciones (vista)", description = "Página principal con el listado de habitaciones y su formulario")
public class RoomWebResource {

    @Inject @Location("index.html") Template indexTemplate;
    @Inject @Location("room-form.html") Template roomFormTemplate;
    @Inject ManageRoomsUseCase rooms;
    @Inject ManageReservationsUseCase reservations;

    // Página principal: resumen del hotel y listado de habitaciones
    @GET
    @Produces(MediaType.TEXT_HTML)
    @APIResponse(responseCode = "200", description = "Página HTML con el resumen del día y las tarjetas de habitaciones")
    @Operation(summary = "Página principal: listado de habitaciones", description = "Muestra el resumen del día (ocupadas, en mantenimiento, llegadas) y las habitaciones con su tipo, precio y estado")
    public TemplateInstance index(@Parameter(hidden = true) @QueryParam("ok") String ok,
                                  @Parameter(hidden = true) @QueryParam("error") String error) {
        LocalDate today = LocalDate.now();
        List<RoomView> roomViews = rooms.findAll().stream()
                .map(room -> WebMapper.toView(room, reservations.isRoomOccupiedOn(room.getId(), today)))
                .toList();
        List<Reservation> all = reservations.findAll();
        long occupied = roomViews.stream().filter(RoomView::occupiedToday).count();
        long maintenance = roomViews.stream().filter(RoomView::maintenance).count();
        long arrivals = all.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED && r.getCheckIn().equals(today))
                .count();
        long active = all.stream().filter(r -> r.getStatus().isActive()).count();

        return indexTemplate.data("page", "rooms")
                .data("rooms", roomViews)
                .data("totalRooms", roomViews.size())
                .data("occupiedToday", occupied)
                .data("maintenanceRooms", maintenance)
                .data("arrivalsToday", arrivals)
                .data("activeReservations", active)
                .data("today", WebMapper.DATE_FORMAT.format(today))
                .data("success", ok)
                .data("error", error);
    }

    // Formulario para registrar una habitación
    @GET
    @Path("/rooms/new")
    @Produces(MediaType.TEXT_HTML)
    @APIResponse(responseCode = "200", description = "Formulario HTML vacío")
    @Operation(summary = "Formulario de nueva habitación")
    public TemplateInstance newRoom() {
        return formPage(WebMapper.emptyRoomForm(), null);
    }

    // Guarda una habitación nueva
    @POST
    @Path("/rooms")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    @APIResponses({
            @APIResponse(responseCode = "303", description = "Habitación registrada: redirige a la página principal con el mensaje de éxito"),
            @APIResponse(responseCode = "200", description = "Regla de negocio incumplida: vuelve al formulario con el mensaje de error (alert-danger)")
    })
    @Operation(summary = "Registrar habitación", description = "Crea la habitación. Reglas: número único de 2 a 4 dígitos, precio mayor a 0 y capacidad según el tipo.")
    public Response create(@Schema(description = "Número de la habitación (2 a 4 dígitos, no repetido)", example = "104") @FormParam("number") String number,
                           @Schema(description = "Tipo de habitación", example = "Doble",
                                   enumeration = {"Sencilla", "Doble", "Familiar", "Suite Dúplex", "Suite Panorámica"}) @FormParam("type") String type,
                           @Schema(description = "Descripción corta", example = "Dos camas dobles y aire acondicionado") @FormParam("description") String description,
                           @Schema(description = "Precio por noche (solo números)", example = "250000") @FormParam("price") String price,
                           @Schema(description = "Huéspedes: Sencilla 1, Doble 2, Suite Panorámica 2, Suite Dúplex 4, Familiar 6", example = "2") @FormParam("capacity") String capacity) {
        RoomForm form = new RoomForm(null, WebMapper.safe(number), WebMapper.safe(type), WebMapper.safe(description),
                WebMapper.safe(price), WebMapper.safe(capacity));
        try {
            BigDecimal pricePerNight = WebMapper.parseMoney(price, "precio por noche");
            int roomCapacity = WebMapper.parseInt(capacity, "capacidad");
            Room room = rooms.create(number, type, description, pricePerNight, roomCapacity);
            return WebResponses.redirectOk("/", "Habitación " + room.getNumber() + " registrada correctamente");
        } catch (BusinessRuleException e) {
            return WebResponses.html(formPage(form, e.getMessage()));
        }
    }

    // Formulario para editar una habitación
    @GET
    @Path("/rooms/{id}/edit")
    @Produces(MediaType.TEXT_HTML)
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Formulario HTML con los datos de la habitación"),
            @APIResponse(responseCode = "404", description = "La habitación no existe")
    })
    @Operation(summary = "Formulario de edición de habitación")
    public TemplateInstance edit(@Parameter(description = "Id de la habitación (datos de prueba: 1=101, 2=102, 3=201, 4=202, 5=203, 6=301, 7=302)", example = "1") @PathParam("id") Long id) {
        return formPage(WebMapper.toForm(rooms.findById(id)), null);
    }

    // Actualiza una habitación existente
    @POST
    @Path("/rooms/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    @APIResponses({
            @APIResponse(responseCode = "303", description = "Habitación actualizada: redirige a la página principal con el mensaje de éxito"),
            @APIResponse(responseCode = "200", description = "Regla de negocio incumplida: vuelve al formulario con el mensaje de error (alert-danger)")
    })
    @Operation(summary = "Actualizar habitación", description = "Modifica los datos de una habitación existente con las mismas reglas del registro.")
    public Response update(@Parameter(description = "Id de la habitación (datos de prueba: 1=101, 2=102, 3=201, 4=202, 5=203, 6=301, 7=302)", example = "1") @PathParam("id") Long id,
                           @Schema(description = "Número de la habitación (2 a 4 dígitos, no repetido)", example = "104") @FormParam("number") String number,
                           @Schema(description = "Tipo de habitación", example = "Doble",
                                   enumeration = {"Sencilla", "Doble", "Familiar", "Suite Dúplex", "Suite Panorámica"}) @FormParam("type") String type,
                           @Schema(description = "Descripción corta", example = "Dos camas dobles y aire acondicionado") @FormParam("description") String description,
                           @Schema(description = "Precio por noche (solo números)", example = "250000") @FormParam("price") String price,
                           @Schema(description = "Huéspedes: Sencilla 1, Doble 2, Suite Panorámica 2, Suite Dúplex 4, Familiar 6", example = "2") @FormParam("capacity") String capacity) {
        RoomForm form = new RoomForm(id, WebMapper.safe(number), WebMapper.safe(type), WebMapper.safe(description),
                WebMapper.safe(price), WebMapper.safe(capacity));
        try {
            BigDecimal pricePerNight = WebMapper.parseMoney(price, "precio por noche");
            int roomCapacity = WebMapper.parseInt(capacity, "capacidad");
            Room room = rooms.update(id, number, type, description, pricePerNight, roomCapacity);
            return WebResponses.redirectOk("/", "Habitación " + room.getNumber() + " actualizada correctamente");
        } catch (BusinessRuleException e) {
            return WebResponses.html(formPage(form, e.getMessage()));
        }
    }

    // Cambia el estado de la habitación: disponible <-> mantenimiento
    @POST
    @Path("/rooms/{id}/maintenance")
    @APIResponses({
            @APIResponse(responseCode = "303", description = "Redirige a la página principal con el mensaje del cambio o el error de la regla de negocio")
    })
    @Operation(summary = "Cambiar estado de mantenimiento", description = "Cambia la habitación de disponible a mantenimiento o al revés. No se permite si tiene un huésped hospedado.")
    public Response toggleMaintenance(@Parameter(description = "Id de la habitación (datos de prueba: 1=101, 2=102, 3=201, 4=202, 5=203, 6=301, 7=302)", example = "1") @PathParam("id") Long id) {
        try {
            Room room = rooms.toggleMaintenance(id);
            String message = room.getStatus() == RoomStatus.MAINTENANCE
                    ? "La habitación " + room.getNumber() + " quedó en mantenimiento"
                    : "La habitación " + room.getNumber() + " quedó disponible";
            return WebResponses.redirectOk("/", message);
        } catch (BusinessRuleException e) {
            return WebResponses.redirectError("/", e.getMessage());
        }
    }

    // Arma la página del formulario (nuevo o edición)
    private TemplateInstance formPage(RoomForm form, String error) {
        return roomFormTemplate.data("page", "rooms")
                .data("form", form)
                .data("editing", form.id() != null)
                .data("types", WebMapper.typeOptions(form.type()))
                .data("success", null)
                .data("error", error);
    }
}
