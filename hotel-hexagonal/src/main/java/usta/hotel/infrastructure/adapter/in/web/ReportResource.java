package usta.hotel.infrastructure.adapter.in.web;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import usta.hotel.application.exception.ResourceNotFoundException;
import usta.hotel.application.port.in.ManageReservationsUseCase;
import usta.hotel.application.port.in.ManageRoomsUseCase;
import usta.hotel.domain.exception.BusinessRuleException;
import usta.hotel.domain.model.Reservation;
import usta.hotel.domain.model.ReservationStatus;
import usta.hotel.domain.model.Room;
import usta.hotel.domain.service.PriceCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Adaptador de entrada WEB para la pantalla de reportes y la descarga de los PDF.
// Usa los mismos puertos de entrada que el resto de la vista.
@Path("/reports")
@ApplicationScoped
@Tag(name = "Reportes", description = "Pantalla de reportes y descarga de reportes en PDF (OpenPDF)")
public class ReportResource {

    private static final String PDF = "application/pdf";

    @Inject @Location("reports.html") Template reportsTemplate;
    @Inject ManageRoomsUseCase rooms;
    @Inject ManageReservationsUseCase reservations;

    private final PriceCalculator priceCalculator = new PriceCalculator();

    // Pantalla de reportes: resumen de las reservas en un rango de fechas
    @GET
    @Produces(MediaType.TEXT_HTML)
    @APIResponse(responseCode = "200", description = "Página HTML con el resumen e ingresos del rango")
    @Operation(summary = "Pantalla de reportes", description = "Resumen de reservas e ingresos por rango de fechas de entrada, con botones para descargar los PDF")
    public TemplateInstance page(@Parameter(description = "Fecha inicial (opcional) (yyyy-MM-dd)", example = "2026-10-01") @QueryParam("from") String from,
                                 @Parameter(description = "Fecha final (opcional) (yyyy-MM-dd)", example = "2026-11-30") @QueryParam("to") String to,
                                 @Parameter(hidden = true) @QueryParam("error") String redirectError) {
        LocalDate today = LocalDate.now();
        String fromText = from == null || from.isBlank() ? today.withDayOfMonth(1).toString() : from;
        String toText = to == null || to.isBlank() ? today.withDayOfMonth(today.lengthOfMonth()).toString() : to;

        TemplateInstance page = reportsTemplate.data("page", "reports")
                .data("from", fromText)
                .data("to", toText)
                .data("success", null);
        try {
            LocalDate fromDate = WebMapper.parseDate(fromText, "fecha inicial");
            LocalDate toDate = WebMapper.parseDate(toText, "fecha final");
            List<Reservation> list = reservations.findByCheckInBetween(fromDate, toDate);

            Map<String, Long> byStatus = new LinkedHashMap<>();
            for (ReservationStatus status : ReservationStatus.values()) {
                byStatus.put(WebMapper.statusLabel(status), list.stream().filter(r -> r.getStatus() == status).count());
            }
            return page.data("reservations", list.stream().map(WebMapper::toView).toList())
                    .data("count", list.size())
                    .data("byStatus", byStatus.entrySet())
                    .data("income", WebMapper.money(income(list)))
                    .data("nights", nightsSold(list))
                    .data("rangeLabel", WebMapper.DATE_FORMAT.format(fromDate) + " al " + WebMapper.DATE_FORMAT.format(toDate))
                    .data("error", redirectError);
        } catch (BusinessRuleException e) {
            return page.data("reservations", List.of())
                    .data("count", 0)
                    .data("byStatus", Map.<String, Long>of().entrySet())
                    .data("income", "0")
                    .data("nights", 0)
                    .data("rangeLabel", "")
                    .data("error", e.getMessage());
        }
    }

    // Descarga el listado de habitaciones en PDF
    @GET
    @Path("/rooms.pdf")
    @Produces(PDF)
    @APIResponse(responseCode = "200", description = "Archivo PDF descargable")
    @Operation(summary = "Reporte PDF de habitaciones", description = "Listado de habitaciones con tipo, capacidad, precio y estado del día")
    public Response roomsPdf() {
        LocalDate today = LocalDate.now();
        List<Room> list = rooms.findAll();
        Map<Long, Boolean> occupied = new LinkedHashMap<>();
        for (Room room : list) {
            occupied.put(room.getId(), reservations.isRoomOccupiedOn(room.getId(), today));
        }
        return pdf(PdfReports.roomsReport(list, occupied), "habitaciones.pdf");
    }

    // Descarga las reservas de un rango de fechas en PDF
    @GET
    @Path("/reservations.pdf")
    @Produces(PDF)
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Archivo PDF descargable"),
            @APIResponse(responseCode = "303", description = "Rango de fechas inválido: redirige a la pantalla de reportes con el mensaje de error")
    })
    @Operation(summary = "Reporte PDF de reservas por rango de fechas", description = "Reservas cuya fecha de entrada está entre 'from' y 'to' (formato yyyy-MM-dd), con el total de ingresos")
    public Response reservationsPdf(@Parameter(description = "Fecha inicial (yyyy-MM-dd)", example = "2026-10-01") @QueryParam("from") String from,
                                    @Parameter(description = "Fecha final (yyyy-MM-dd)", example = "2026-11-30") @QueryParam("to") String to) {
        try {
            LocalDate fromDate = WebMapper.parseDate(from, "fecha inicial");
            LocalDate toDate = WebMapper.parseDate(to, "fecha final");
            List<Reservation> list = reservations.findByCheckInBetween(fromDate, toDate);
            return pdf(PdfReports.reservationsReport(fromDate, toDate, list, income(list)),
                    "reservas-" + fromDate + "-a-" + toDate + ".pdf");
        } catch (BusinessRuleException e) {
            return WebResponses.redirectError("/reports", e.getMessage());
        }
    }

    // Descarga el comprobante de una reserva en PDF (404 si no existe)
    @GET
    @Path("/receipt/{id}.pdf")
    @Produces(PDF)
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Archivo PDF del comprobante"),
            @APIResponse(responseCode = "404", description = "La reserva no existe")
    })
    @Operation(summary = "Comprobante PDF de una reserva", description = "Comprobante con los datos del huésped, la habitación y el desglose del cobro")
    public Response receiptPdf(@Parameter(description = "Id de la reserva (1 a 8)", example = "1") @PathParam("id") Long id) {
        Reservation reservation = reservations.findById(id);
        if (reservation == null) {
            throw new ResourceNotFoundException("No existe la reserva con id " + id);
        }
        return pdf(PdfReports.receipt(reservation), "comprobante-reserva-" + id + ".pdf");
    }

    // Ingresos: suma de las reservas que no están canceladas
    private BigDecimal income(List<Reservation> list) {
        BigDecimal total = BigDecimal.ZERO;
        for (Reservation reservation : list) {
            if (reservation.getStatus() != ReservationStatus.CANCELLED) {
                total = total.add(reservation.getTotal());
            }
        }
        return total;
    }

    // Noches vendidas (sin contar las canceladas)
    private long nightsSold(List<Reservation> list) {
        long nights = 0;
        for (Reservation reservation : list) {
            if (reservation.getStatus() != ReservationStatus.CANCELLED) {
                nights += priceCalculator.nights(reservation.getCheckIn(), reservation.getCheckOut());
            }
        }
        return nights;
    }

    private Response pdf(byte[] content, String fileName) {
        return Response.ok(content)
                .type(PDF)
                .header("Content-Disposition", "attachment; filename=" + fileName)
                .build();
    }
}
