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
import usta.hotel.application.port.in.ManageGuestsUseCase;
import usta.hotel.domain.exception.BusinessRuleException;
import usta.hotel.domain.model.Guest;
import usta.hotel.infrastructure.adapter.in.web.WebMapper.GuestForm;

// Adaptador de entrada WEB: listado y registro de huéspedes
@Path("/guests")
@ApplicationScoped
@Tag(name = "Huéspedes (vista)", description = "Listado y formulario de registro de huéspedes")
public class GuestWebResource {

    @Inject @Location("guests.html") Template guestsTemplate;
    @Inject ManageGuestsUseCase guests;

    // Listado de huéspedes con el formulario de registro
    @GET
    @Produces(MediaType.TEXT_HTML)
    @APIResponse(responseCode = "200", description = "Página HTML con la tabla de huéspedes y el formulario de registro")
    @Operation(summary = "Listado de huéspedes", description = "Muestra los huéspedes registrados y el formulario para registrar uno nuevo")
    public TemplateInstance list(@Parameter(hidden = true) @QueryParam("ok") String ok,
                                 @Parameter(hidden = true) @QueryParam("error") String error) {
        return page(WebMapper.emptyGuestForm(), ok, error);
    }

    // Registra un huésped nuevo
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    @APIResponses({
            @APIResponse(responseCode = "303", description = "Huésped registrado: redirige al listado de huéspedes con el mensaje de éxito"),
            @APIResponse(responseCode = "200", description = "Regla de negocio incumplida: vuelve al formulario con el mensaje de error (alert-danger)")
    })
    @Operation(summary = "Registrar huésped", description = "Registra un huésped. Reglas: documento de 5 a 15 dígitos y no repetido, correo válido y no repetido, teléfono de 7 a 15 dígitos")
    public Response register(@Schema(description = "Documento (5 a 15 dígitos, no repetido)", example = "1049876543") @FormParam("document") String document,
                             @Schema(description = "Nombre", example = "Ana") @FormParam("firstName") String firstName,
                             @Schema(description = "Apellidos", example = "Ruiz Torres") @FormParam("lastName") String lastName,
                             @Schema(description = "Correo electrónico (no repetido)", example = "ana.ruiz@correo.com") @FormParam("email") String email,
                             @Schema(description = "Teléfono (7 a 15 dígitos)", example = "3001234567") @FormParam("phone") String phone,
                             @Schema(description = "Ciudad", example = "Tunja") @FormParam("city") String city) {
        GuestForm form = new GuestForm(WebMapper.safe(document), WebMapper.safe(firstName), WebMapper.safe(lastName),
                WebMapper.safe(email), WebMapper.safe(phone), WebMapper.safe(city));
        try {
            Guest guest = guests.register(document, firstName, lastName, email, phone, city);
            return WebResponses.redirectOk("/guests", "Huésped " + guest.fullName() + " registrado correctamente");
        } catch (BusinessRuleException e) {
            return WebResponses.html(page(form, null, e.getMessage()));
        }
    }

    private TemplateInstance page(GuestForm form, String success, String error) {
        return guestsTemplate.data("page", "guests")
                .data("guests", guests.findAll().stream().map(WebMapper::toView).toList())
                .data("form", form)
                .data("success", success)
                .data("error", error);
    }
}
