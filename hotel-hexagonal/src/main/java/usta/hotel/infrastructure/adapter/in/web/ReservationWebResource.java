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
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import usta.hotel.application.exception.ResourceNotFoundException;
import usta.hotel.application.port.in.ManageGuestsUseCase;
import usta.hotel.application.port.in.ManageReservationsUseCase;
import usta.hotel.application.port.in.ManageRoomsUseCase;
import usta.hotel.domain.exception.BusinessRuleException;
import usta.hotel.domain.model.Reservation;
import usta.hotel.domain.model.ReservationStatus;
import usta.hotel.infrastructure.adapter.in.web.WebMapper.ReservationForm;
import usta.hotel.infrastructure.adapter.in.web.WebMapper.StatusFilter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

// Adaptador de entrada WEB: listado de reservas, formulario de reserva, detalle y cambios de estado
@Path("/reservations")
@ApplicationScoped
@Tag(name = "Reservas (vista)", description = "Listado, formulario, detalle y cambios de estado de las reservas")
public class ReservationWebResource {

    @Inject @Location("reservations.html") Template listTemplate;
    @Inject @Location("reservation-form.html") Template formTemplate;
    @Inject @Location("reservation-detail.html") Template detailTemplate;
    @Inject ManageReservationsUseCase reservations;
    @Inject ManageRoomsUseCase rooms;
    @Inject ManageGuestsUseCase guests;

    // Listado de reservas con filtro por estado
    @GET
    @Produces(MediaType.TEXT_HTML)
    @APIResponse(responseCode = "200", description = "Página HTML con la tabla de reservas filtrada")
    @Operation(summary = "Listado de reservas", description = "Muestra las reservas; se puede filtrar por estado (CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED)")
    public TemplateInstance list(@Parameter(description = "Filtro por estado (vacío = todas)",
                                         schema = @Schema(type = SchemaType.STRING,
                                                 enumeration = {"CONFIRMED", "CHECKED_IN", "COMPLETED", "CANCELLED"}))
                                 @QueryParam("status") String status,
                                 @Parameter(hidden = true) @QueryParam("ok") String ok,
                                 @Parameter(hidden = true) @QueryParam("error") String error) {
        List<Reservation> all = reservations.findAll();
        ReservationStatus selected = parseStatus(status);

        List<StatusFilter> filters = new ArrayList<>();
        filters.add(new StatusFilter("", "Todas", selected == null, all.size()));
        for (ReservationStatus value : ReservationStatus.values()) {
            long count = all.stream().filter(r -> r.getStatus() == value).count();
            filters.add(new StatusFilter(value.name(), WebMapper.statusLabel(value), value == selected, count));
        }

        List<WebMapper.ReservationView> views = all.stream()
                .filter(r -> selected == null || r.getStatus() == selected)
                .map(WebMapper::toView)
                .toList();

        return listTemplate.data("page", "reservations")
                .data("reservations", views)
                .data("filters", filters)
                .data("back", selected == null ? "/reservations" : "/reservations?status=" + selected.name())
                .data("success", ok)
                .data("error", error);
    }

    // Formulario para crear una reserva (puede venir con la habitación preseleccionada)
    @GET
    @Path("/new")
    @Produces(MediaType.TEXT_HTML)
    @APIResponse(responseCode = "200", description = "Formulario HTML de reserva")
    @Operation(summary = "Formulario de nueva reserva")
    public TemplateInstance newReservation(@Parameter(description = "Habitación preseleccionada (opcional)", example = "7") @QueryParam("roomId") String roomId,
                                           @Parameter(description = "Huésped preseleccionado (opcional)", example = "2") @QueryParam("guestId") String guestId) {
        LocalDate today = LocalDate.now();
        ReservationForm form = new ReservationForm(WebMapper.safe(guestId), WebMapper.safe(roomId),
                today.toString(), today.plusDays(1).toString(), "1");
        return formPage(form, null);
    }

    // Crea la reserva; las reglas de negocio las valida el dominio
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    @APIResponses({
            @APIResponse(responseCode = "303", description = "Reserva creada: redirige al detalle de la reserva con el mensaje de éxito y el total"),
            @APIResponse(responseCode = "200", description = "Regla de negocio incumplida: vuelve al formulario con el mensaje de error (alert-danger)")
    })
    @Operation(summary = "Crear reserva", description = "Crea la reserva y calcula el total. Reglas: entrada desde hoy, salida posterior a la entrada, máximo 30 noches, no supera la capacidad, la habitación no está en mantenimiento y no se cruza con otra reserva activa")
    public Response create(@Schema(description = "Id del huésped (1=Laura, 2=Carlos, 3=Andrea, 4=Jorge, 5=Sofía)", example = "2") @FormParam("guestId") String guestId,
                           @Schema(description = "Id de la habitación (1=101, 2=102, 3=201, 4=202, 5=203, 6=301, 7=302)", example = "7") @FormParam("roomId") String roomId,
                           @Schema(description = "Fecha de entrada (yyyy-MM-dd, desde hoy)", example = "2026-11-20") @FormParam("checkIn") String checkIn,
                           @Schema(description = "Fecha de salida (yyyy-MM-dd, posterior a la entrada)", example = "2026-11-22") @FormParam("checkOut") String checkOut,
                           @Schema(description = "Cantidad de huéspedes (no mayor a la capacidad)", example = "2") @FormParam("guestsCount") String guestsCount) {
        ReservationForm form = new ReservationForm(WebMapper.safe(guestId), WebMapper.safe(roomId),
                WebMapper.safe(checkIn), WebMapper.safe(checkOut), WebMapper.safe(guestsCount));
        try {
            Reservation reservation = reservations.book(
                    WebMapper.parseId(guestId),
                    WebMapper.parseId(roomId),
                    WebMapper.parseDate(checkIn, "fecha de entrada"),
                    WebMapper.parseDate(checkOut, "fecha de salida"),
                    WebMapper.parseInt(guestsCount, "cantidad de huéspedes"));
            return WebResponses.redirectOk("/reservations/" + reservation.getId(),
                    "Reserva #" + reservation.getId() + " creada correctamente. Total: $"
                            + WebMapper.money(reservation.getTotal()));
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            return WebResponses.html(formPage(form, e.getMessage()));
        }
    }

    // Detalle de una reserva con el desglose de la cuenta
    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_HTML)
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Página HTML con el detalle y la cuenta"),
            @APIResponse(responseCode = "404", description = "La reserva no existe")
    })
    @Operation(summary = "Detalle de la reserva", description = "Muestra los datos de la reserva, el desglose del cobro y las acciones disponibles")
    public TemplateInstance detail(@Parameter(description = "Id de la reserva (datos de prueba: 1 a 8)", example = "1") @PathParam("id") Long id,
                                   @Parameter(hidden = true) @QueryParam("ok") String ok,
                                   @Parameter(hidden = true) @QueryParam("error") String error) {
        Reservation reservation = reservations.findById(id);
        return detailTemplate.data("page", "reservations")
                .data("r", WebMapper.toView(reservation))
                .data("bill", WebMapper.toBill(reservation))
                .data("guest", WebMapper.toView(reservation.getGuest()))
                .data("photoUrl", WebMapper.photoUrl(reservation.getRoom().getType()))
                .data("typeIcon", WebMapper.typeIcon(reservation.getRoom().getType()))
                .data("back", "/reservations/" + id)
                .data("success", ok)
                .data("error", error);
    }

    // Registra la llegada del huésped (CONFIRMED -> CHECKED_IN)
    @POST
    @Path("/{id}/check-in")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @APIResponses({
            @APIResponse(responseCode = "303", description = "Redirige al listado de reservas con el mensaje de éxito o de error de la regla de negocio")
    })
    @Operation(summary = "Check-in", description = "Registra la llegada del huésped: CONFIRMED -> CHECKED_IN. Solo desde la fecha de entrada y antes de la salida")
    public Response checkIn(@Parameter(description = "Id de la reserva (datos de prueba: 1 a 8)", example = "2") @PathParam("id") Long id, @Parameter(hidden = true) @FormParam("from") String from) {
        return changeStatus(id, from, reservations::checkIn, "Check-in registrado para la reserva #" + id);
    }

    // Registra la salida del huésped (CHECKED_IN -> COMPLETED)
    @POST
    @Path("/{id}/check-out")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @APIResponses({
            @APIResponse(responseCode = "303", description = "Redirige al listado de reservas con el mensaje de éxito o de error de la regla de negocio")
    })
    @Operation(summary = "Check-out", description = "Registra la salida del huésped: CHECKED_IN -> COMPLETED. Solo si el huésped está hospedado")
    public Response checkOut(@Parameter(description = "Id de la reserva (datos de prueba: 1 a 8)", example = "1") @PathParam("id") Long id, @Parameter(hidden = true) @FormParam("from") String from) {
        return changeStatus(id, from, reservations::checkOut, "Check-out registrado para la reserva #" + id);
    }

    // Cancela la reserva (CONFIRMED -> CANCELLED)
    @POST
    @Path("/{id}/cancel")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @APIResponses({
            @APIResponse(responseCode = "303", description = "Redirige al listado de reservas con el mensaje de éxito o de error de la regla de negocio")
    })
    @Operation(summary = "Cancelar reserva", description = "Cancela la reserva: CONFIRMED -> CANCELLED. Solo se pueden cancelar reservas confirmadas")
    public Response cancel(@Parameter(description = "Id de la reserva (datos de prueba: 1 a 8)", example = "3") @PathParam("id") Long id, @Parameter(hidden = true) @FormParam("from") String from) {
        return changeStatus(id, from, reservations::cancel, "La reserva #" + id + " fue cancelada");
    }

    // Ejecuta el cambio de estado y vuelve a la página de origen con el mensaje
    private Response changeStatus(Long id, String from, Function<Long, Reservation> action, String message) {
        String back = WebResponses.safeBack(from, "/reservations");
        try {
            action.apply(id);
            return WebResponses.redirectOk(back, message);
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            return WebResponses.redirectError(back, e.getMessage());
        }
    }

    private TemplateInstance formPage(ReservationForm form, String error) {
        return formTemplate.data("page", "reservations")
                .data("form", form)
                .data("guestOptions", WebMapper.guestOptions(guests.findAll(), form.guestId()))
                .data("roomOptions", WebMapper.roomOptions(rooms.findAll(), form.roomId()))
                .data("today", LocalDate.now().toString())
                .data("success", null)
                .data("error", error);
    }

    private ReservationStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ReservationStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
