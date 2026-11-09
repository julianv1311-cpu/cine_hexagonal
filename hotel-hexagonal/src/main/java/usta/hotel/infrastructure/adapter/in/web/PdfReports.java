package usta.hotel.infrastructure.adapter.in.web;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.DottedLineSeparator;
import usta.hotel.domain.model.Reservation;
import usta.hotel.domain.model.ReservationStatus;
import usta.hotel.domain.model.Room;
import usta.hotel.domain.model.RoomStatus;
import usta.hotel.domain.service.PriceCalculator;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Arma los documentos PDF (habitaciones, reservas por rango y comprobante) con OpenPDF
public final class PdfReports {

    // Colores del reporte
    private static final Color NAVY = new Color(0x1F, 0x3A, 0x5F);
    private static final Color GOLD = new Color(0xB8, 0x86, 0x0B);
    private static final Color GRAY = new Color(0x60, 0x60, 0x60);
    private static final Color DARK = new Color(0x33, 0x33, 0x33);
    private static final Color LIGHT = new Color(0xF2, 0xF4, 0xF7);
    private static final Color GREEN = new Color(0x27, 0xAE, 0x60);
    private static final Color RED = new Color(0xC0, 0x39, 0x2B);

    // Comprobante: página pequeña tipo tirilla y fuentes compactas
    private static final Rectangle TICKET = new Rectangle(250f, 520f);
    private static final Font R_BIG = new Font(Font.HELVETICA, 15, Font.BOLD, NAVY);
    private static final Font R_TITLE = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font R_TEXT = new Font(Font.HELVETICA, 8);
    private static final Font R_BOLD = new Font(Font.HELVETICA, 8, Font.BOLD);
    private static final Font R_TOTAL = new Font(Font.HELVETICA, 11, Font.BOLD);
    private static final Font R_FOOT = new Font(Font.HELVETICA, 7, Font.ITALIC);

    private PdfReports() {
    }

    // Reporte de habitaciones: número, tipo, capacidad, precio y estado del día
    public static byte[] roomsReport(List<Room> rooms, Map<Long, Boolean> occupiedToday) {
        Document document = new Document(PageSize.LETTER, 42, 42, 42, 42);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, output);
            document.open();
            header(document, "HABITACIONES", "Hotel Westin - estado de las habitaciones");

            PdfPTable table = new PdfPTable(new float[]{1.0f, 1.5f, 3.6f, 1.2f, 1.6f, 1.7f});
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            for (String title : List.of("Número", "Tipo", "Descripción", "Capacidad", "Precio noche", "Estado hoy")) {
                table.addCell(headerCell(title));
            }

            boolean alternate = false;
            long available = 0;
            for (Room room : rooms) {
                Color background = alternate ? LIGHT : Color.WHITE;
                boolean occupied = Boolean.TRUE.equals(occupiedToday.get(room.getId()));
                String status;
                if (room.getStatus() == RoomStatus.MAINTENANCE) {
                    status = "Mantenimiento";
                } else if (occupied) {
                    status = "Ocupada";
                } else {
                    status = "Disponible";
                    available++;
                }
                table.addCell(bodyCell(room.getNumber(), Element.ALIGN_CENTER, background));
                table.addCell(bodyCell(room.getType(), Element.ALIGN_LEFT, background));
                table.addCell(bodyCell(room.getDescription() == null ? "" : room.getDescription(), Element.ALIGN_LEFT, background));
                table.addCell(bodyCell(String.valueOf(room.getCapacity()), Element.ALIGN_CENTER, background));
                table.addCell(bodyCell("$" + WebMapper.money(room.getPricePerNight()), Element.ALIGN_RIGHT, background));
                table.addCell(bodyCell(status, Element.ALIGN_CENTER, background));
                alternate = !alternate;
            }
            document.add(table);

            document.add(footer("Total de habitaciones: " + rooms.size() + "   |   Disponibles hoy: " + available));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF de habitaciones", e);
        } finally {
            document.close();
        }
        return output.toByteArray();
    }

    // Reporte de reservas por rango de fechas de entrada, con el total de ingresos
    public static byte[] reservationsReport(LocalDate from, LocalDate to, List<Reservation> reservations,
                                            BigDecimal income) {
        Document document = new Document(PageSize.LETTER.rotate(), 36, 36, 36, 36);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PriceCalculator calculator = new PriceCalculator();
        try {
            PdfWriter.getInstance(document, output);
            document.open();
            header(document, "RESERVAS POR RANGO DE FECHAS",
                    "Entradas del " + WebMapper.DATE_FORMAT.format(from) + " al " + WebMapper.DATE_FORMAT.format(to));

            PdfPTable table = new PdfPTable(new float[]{0.8f, 3.0f, 1.6f, 1.6f, 1.6f, 1.6f, 1.0f, 1.0f, 1.6f, 1.8f});
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            for (String title : List.of("#", "Huésped", "Documento", "Habitación", "Entrada", "Salida",
                    "Noches", "Pers.", "Estado", "Total")) {
                table.addCell(headerCell(title));
            }

            boolean alternate = false;
            for (Reservation r : reservations) {
                Color background = alternate ? LIGHT : Color.WHITE;
                table.addCell(bodyCell(String.valueOf(r.getId()), Element.ALIGN_CENTER, background));
                table.addCell(bodyCell(r.getGuest().fullName(), Element.ALIGN_LEFT, background));
                table.addCell(bodyCell(r.getGuest().getDocument(), Element.ALIGN_CENTER, background));
                table.addCell(bodyCell(r.getRoom().getNumber() + " - " + r.getRoom().getType(), Element.ALIGN_LEFT, background));
                table.addCell(bodyCell(WebMapper.DATE_FORMAT.format(r.getCheckIn()), Element.ALIGN_CENTER, background));
                table.addCell(bodyCell(WebMapper.DATE_FORMAT.format(r.getCheckOut()), Element.ALIGN_CENTER, background));
                table.addCell(bodyCell(String.valueOf(calculator.nights(r.getCheckIn(), r.getCheckOut())), Element.ALIGN_CENTER, background));
                table.addCell(bodyCell(String.valueOf(r.getGuestsCount()), Element.ALIGN_CENTER, background));
                table.addCell(bodyCell(WebMapper.statusLabel(r.getStatus()), Element.ALIGN_CENTER, background));
                table.addCell(bodyCell("$" + WebMapper.money(r.getTotal()), Element.ALIGN_RIGHT, background));
                alternate = !alternate;
            }
            if (reservations.isEmpty()) {
                PdfPCell empty = bodyCell("No hay reservas con fecha de entrada en este rango", Element.ALIGN_CENTER, Color.WHITE);
                empty.setColspan(10);
                table.addCell(empty);
            }
            document.add(table);

            document.add(footer("Total de reservas: " + reservations.size()
                    + "   |   Ingresos (sin canceladas): $" + WebMapper.money(income)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF de reservas", e);
        } finally {
            document.close();
        }
        return output.toByteArray();
    }

    // Comprobante de la reserva en formato de tirilla
    public static byte[] receipt(Reservation reservation) {
        Document document = new Document(TICKET, 16, 16, 16, 16);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, output);
            document.open();

            // Encabezado del negocio
            document.add(centered("HOTEL WESTIN", R_BIG));
            document.add(centered("NIT 900.987.654-3", R_TEXT));
            document.add(centered("Avenida Norte # 45-10 - Tunja", R_TEXT));
            document.add(dotted());
            document.add(centered("COMPROBANTE DE RESERVA", R_TITLE));
            document.add(centered("No. " + String.format("%06d", reservation.getId()), R_BOLD));
            if (reservation.getCreatedAt() != null) {
                document.add(centered("Registrada el " + WebMapper.DATE_TIME_FORMAT.format(reservation.getCreatedAt()), R_TEXT));
            }
            document.add(dotted());

            // Datos del huésped y de la estadía
            document.add(line("Huésped", reservation.getGuest().fullName()));
            document.add(line("Documento", reservation.getGuest().getDocument()));
            document.add(line("Habitación", reservation.getRoom().getNumber() + " - " + reservation.getRoom().getType()));
            document.add(line("Entrada", WebMapper.DATE_FORMAT.format(reservation.getCheckIn())));
            document.add(line("Salida", WebMapper.DATE_FORMAT.format(reservation.getCheckOut())));
            document.add(line("Personas", String.valueOf(reservation.getGuestsCount())));
            document.add(dotted());

            // Cuenta: noches, recargo de fin de semana y descuento
            WebMapper.BillView bill = WebMapper.toBill(reservation);
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 2f});
            table.addCell(cellLeft(bill.nights() + " noches x $" + bill.pricePerNight(), R_TEXT));
            table.addCell(cellRight("", R_TEXT));
            if (bill.weekendNights() > 0) {
                table.addCell(cellLeft("Recargo fin de semana (" + bill.weekendNights() + " noches, 15%)", R_TEXT));
                table.addCell(cellRight("incluido", R_TEXT));
            }
            table.addCell(cellLeft("Subtotal", R_TEXT));
            table.addCell(cellRight("$" + bill.subtotal(), R_TEXT));
            if (bill.hasDiscount()) {
                table.addCell(cellLeft("Descuento estadía larga (10%)", R_TEXT));
                table.addCell(cellRight("-$" + bill.discount(), R_TEXT));
            }
            document.add(table);
            document.add(dotted());
            document.add(line("TOTAL", "$" + bill.total(), R_TOTAL));
            document.add(dotted());

            // Estado de la reserva
            Color color = reservation.getStatus() == ReservationStatus.CANCELLED ? RED : GREEN;
            document.add(centered("ESTADO: " + WebMapper.statusLabel(reservation.getStatus()).toUpperCase(),
                    new Font(Font.HELVETICA, 9, Font.BOLD, color)));
            document.add(dotted());
            document.add(centered("Check-in desde las 3:00 p. m. - Check-out hasta las 12:00 m.", R_FOOT));
            document.add(centered("Gracias por preferirnos", R_FOOT));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF del comprobante", e);
        } finally {
            document.close();
        }
        return output.toByteArray();
    }

    // Título, subtítulo y fecha de generación del reporte
    private static void header(Document document, String titleText, String subtitleText) throws Exception {
        Paragraph title = new Paragraph(titleText, new Font(Font.HELVETICA, 20, Font.BOLD, NAVY));
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph(subtitleText, new Font(Font.HELVETICA, 10, Font.ITALIC, GOLD));
        subtitle.setAlignment(Element.ALIGN_CENTER);
        document.add(subtitle);

        Paragraph generated = new Paragraph("Generado el " + WebMapper.DATE_TIME_FORMAT.format(LocalDateTime.now()),
                new Font(Font.HELVETICA, 8, Font.NORMAL, GRAY));
        generated.setAlignment(Element.ALIGN_RIGHT);
        generated.setSpacingBefore(8);
        generated.setSpacingAfter(8);
        document.add(generated);
    }

    private static Paragraph footer(String text) {
        Paragraph footer = new Paragraph(text, new Font(Font.HELVETICA, 9, Font.BOLD, DARK));
        footer.setSpacingBefore(10);
        return footer;
    }

    // Celda de encabezado de la tabla (fondo azul, texto blanco)
    private static PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
        cell.setBackgroundColor(NAVY);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    // Celda de datos de la tabla
    private static PdfPCell bodyCell(String text, int alignment, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(Font.HELVETICA, 8, Font.NORMAL, DARK)));
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(background);
        return cell;
    }

    private static Paragraph centered(String text, Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setSpacingAfter(1);
        return paragraph;
    }

    private static Paragraph line(String label, String value) {
        return line(label, value, R_TEXT);
    }

    private static Paragraph line(String label, String value, Font font) {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Chunk(label + ": ", R_BOLD));
        paragraph.add(new Chunk(value, font));
        paragraph.setSpacingAfter(1);
        return paragraph;
    }

    // Separador punteado tipo tirilla
    private static Paragraph dotted() {
        DottedLineSeparator separator = new DottedLineSeparator();
        separator.setGap(1.5f);
        Paragraph paragraph = new Paragraph();
        paragraph.setSpacingBefore(3);
        paragraph.setSpacingAfter(3);
        paragraph.add(new Chunk(separator));
        return paragraph;
    }

    private static PdfPCell cellLeft(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(1);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }

    private static PdfPCell cellRight(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(1);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }
}
