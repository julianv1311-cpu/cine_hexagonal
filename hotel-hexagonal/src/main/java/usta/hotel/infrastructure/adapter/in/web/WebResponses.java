package usta.hotel.infrastructure.adapter.in.web;

import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// Utilidades del adaptador web: redirecciones con mensajes y respuestas HTML
public final class WebResponses {

    private WebResponses() {
    }

    // Redirige (303) a una ruta con un mensaje de éxito
    public static Response redirectOk(String path, String message) {
        return redirect(path, "ok", message);
    }

    // Redirige (303) a una ruta con un mensaje de error
    public static Response redirectError(String path, String message) {
        return redirect(path, "error", message);
    }

    // Devuelve una página HTML ya armada (por ejemplo un formulario con su error)
    public static Response html(TemplateInstance page) {
        return Response.ok(page.render()).type(MediaType.TEXT_HTML_TYPE.withCharset("UTF-8")).build();
    }

    // Solo permite volver a una ruta interna (evita redirecciones externas)
    public static String safeBack(String from, String fallback) {
        if (from != null && from.startsWith("/") && !from.startsWith("//")) {
            return from;
        }
        return fallback;
    }

    private static Response redirect(String path, String param, String message) {
        String separator = path.contains("?") ? "&" : "?";
        String target = path + separator + param + "=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
        return Response.seeOther(URI.create(target)).build();
    }
}
