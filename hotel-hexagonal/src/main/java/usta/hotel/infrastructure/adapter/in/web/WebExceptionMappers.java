package usta.hotel.infrastructure.adapter.in.web;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import usta.hotel.application.exception.ResourceNotFoundException;
import usta.hotel.domain.exception.BusinessRuleException;

// Convierte las excepciones que no se manejaron en un recurso en una página HTML de error
public class WebExceptionMappers {

    @Inject @Location("error.html") Template errorTemplate;

    // Recurso inexistente (por ejemplo /reservations/999) -> página 404
    @ServerExceptionMapper
    public Response notFound(ResourceNotFoundException e) {
        return page(Response.Status.NOT_FOUND, "No encontrado", e.getMessage());
    }

    // Regla de negocio incumplida que no se mostró en un formulario -> página 400
    @ServerExceptionMapper
    public Response businessRule(BusinessRuleException e) {
        return page(Response.Status.BAD_REQUEST, "Operación no permitida", e.getMessage());
    }

    private Response page(Response.Status status, String title, String message) {
        String html = errorTemplate.data("page", "")
                .data("code", status.getStatusCode())
                .data("errorTitle", title)
                .data("message", message)
                .data("success", null)
                .data("error", null)
                .render();
        return Response.status(status).entity(html).type(MediaType.TEXT_HTML_TYPE.withCharset("UTF-8")).build();
    }
}
