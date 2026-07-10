package pe.edu.upc.medibridge.reportsanalytics.infrastructure.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;
import pe.edu.upc.medibridge.reportsanalytics.domain.model.aggregates.ClinicalReport;
import pe.edu.upc.medibridge.reportsanalytics.domain.model.entities.ReportSection;
import pe.edu.upc.medibridge.reportsanalytics.domain.model.exceptions.ReportGenerationException;
import pe.edu.upc.medibridge.reportsanalytics.domain.model.valueobjects.ReportType;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

@Component
public class ITextPdfReportGenerator {
    private static final Color PRIMARY_COLOR = new Color(28, 55, 84);
    private static final Color SECONDARY_COLOR = new Color(233, 239, 245);
    private static final Color BORDER_COLOR = new Color(180, 190, 200);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId PERU_ZONE = ZoneId.of("America/Lima");

    public byte[] generate(ClinicalReport report) {
        try {
            var outputStream = new ByteArrayOutputStream();
            var document = new Document(PageSize.A4, 42, 42, 42, 42);
            PdfWriter.getInstance(document, outputStream);

            document.open();
            addHeader(document);
            addReportMetadata(document, report);
            addSummary(document, report);
            addClinicalTable(document, report);
            addClosingNotes(document);
            document.close();

            return outputStream.toByteArray();
        } catch (DocumentException exception) {
            throw new ReportGenerationException("Unable to generate clinical report PDF", exception);
        } catch (RuntimeException exception) {
            throw new ReportGenerationException("Unable to generate clinical report PDF", exception);
        }
    }

    private void addHeader(Document document) throws DocumentException {
        Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY_COLOR);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
        Font documentTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, Color.BLACK);

        var brand = new Paragraph("MediBridge", brandFont);
        brand.setAlignment(Element.ALIGN_CENTER);
        brand.setSpacingAfter(2);
        document.add(brand);

        var subtitle = new Paragraph("Plataforma de gestion de cuidado integral", subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(14);
        document.add(subtitle);

        var title = new Paragraph("Reporte clinico para la familia", documentTitleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(18);
        document.add(title);
    }

    private void addReportMetadata(Document document, ClinicalReport report) throws DocumentException {
        addSectionTitle(document, "Datos del reporte");

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

        var table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{28, 72});
        table.setSpacingAfter(16);

        addMetadataRow(table, "Codigo de reporte", valueOrPending(report.getId()), headerFont, bodyFont);
        addMetadataRow(table, "Referencia del paciente", valueOrPending(report.getPatientId()), headerFont, bodyFont);
        addMetadataRow(table, "Tipo de reporte", describeReportType(report.getReportType()), headerFont, bodyFont);
        addMetadataRow(table, "Periodo evaluado", formatDate(report.getPeriodStartDate()) + " al " + formatDate(report.getPeriodEndDate()), headerFont, bodyFont);
        addMetadataRow(table, "Generado el", formatDateTime(report.getGeneratedAt()), headerFont, bodyFont);

        document.add(table);
    }

    private void addMetadataRow(PdfPTable table, String label, String value, Font headerFont, Font bodyFont) {
        var labelCell = new PdfPCell(new Phrase(label, headerFont));
        labelCell.setBackgroundColor(PRIMARY_COLOR);
        labelCell.setPadding(8);
        labelCell.setBorderColor(BORDER_COLOR);
        table.addCell(labelCell);

        var valueCell = new PdfPCell(new Phrase(value, bodyFont));
        valueCell.setPadding(8);
        valueCell.setBorderColor(BORDER_COLOR);
        table.addCell(valueCell);
    }

    private void addSummary(Document document, ClinicalReport report) throws DocumentException {
        addSectionTitle(document, "Resumen para la familia");

        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

        var summary = new Paragraph(textForPdf(report.getSummary()), bodyFont);
        summary.setSpacingAfter(14);
        summary.setLeading(14);
        document.add(summary);
    }

    private void addClinicalTable(Document document, ClinicalReport report) throws DocumentException {
        addSectionTitle(document, "Ficha clinica");

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

        var table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{30, 70});
        table.setSpacingAfter(16);

        addTableHeader(table, "Area de cuidado", headerFont);
        addTableHeader(table, "Informacion relevante", headerFont);

        var sections = report.getSections().stream()
                .sorted(Comparator.comparing(ReportSection::getDisplayOrder))
                .toList();
        if (sections.isEmpty()) {
            addTableBodyCell(table, "Sin secciones", bodyFont);
            addTableBodyCell(table, "No hay informacion clinica registrada para este reporte.", bodyFont);
        } else {
            sections.forEach(section -> {
                addTableBodyCell(table, translateSectionTitle(section.getTitle()), bodyFont);
                addTableBodyCell(table, textForPdf(section.getContent()), bodyFont);
            });
        }

        document.add(table);
    }

    private void addTableHeader(PdfPTable table, String label, Font font) {
        var cell = new PdfPCell(new Phrase(label, font));
        cell.setBackgroundColor(PRIMARY_COLOR);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private void addTableBodyCell(PdfPTable table, String value, Font font) {
        var cell = new PdfPCell(new Phrase(value, font));
        cell.setPadding(8);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private void addSectionTitle(Document document, String title) throws DocumentException {
        Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, PRIMARY_COLOR);
        var paragraph = new Paragraph(title, sectionTitleFont);
        paragraph.setSpacingBefore(8);
        paragraph.setSpacingAfter(8);
        document.add(paragraph);
    }

    private void addClosingNotes(Document document) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, PRIMARY_COLOR);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);

        var table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);

        var titleCell = new PdfPCell(new Phrase("Nota para la familia", titleFont));
        titleCell.setBackgroundColor(SECONDARY_COLOR);
        titleCell.setPadding(8);
        titleCell.setBorderColor(BORDER_COLOR);
        table.addCell(titleCell);

        var note = "Este reporte resume la informacion disponible en MediBridge al momento de generarlo. "
                + "No reemplaza la evaluacion de un profesional de salud ante sintomas nuevos, dolor intenso o una emergencia.";
        var noteCell = new PdfPCell(new Phrase(note, bodyFont));
        noteCell.setPadding(8);
        noteCell.setBorderColor(BORDER_COLOR);
        table.addCell(noteCell);

        document.add(table);
    }

    private String formatDate(java.time.LocalDate date) {
        if (date == null) {
            return "No registrado";
        }
        return date.format(DATE_FORMAT);
    }

    static String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "No registrado";
        }
        return dateTime
                .atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(PERU_ZONE)
                .format(DATE_TIME_FORMAT);
    }

    private String valueOrPending(Object value) {
        if (value == null) {
            return "No registrado";
        }
        var text = String.valueOf(value);
        return text.isBlank() ? "No registrado" : text;
    }

    private String describeReportType(ReportType reportType) {
        return switch (reportType) {
            case VITAL_SIGNS -> "Signos vitales";
            case MEDICATION -> "Medicacion";
            case FULL_CLINICAL -> "Clinico completo";
        };
    }

    private String translateSectionTitle(String title) {
        return switch (valueOrPending(title)) {
            case "Patient overview" -> "Datos del paciente";
            case "Health monitoring" -> "Signos vitales y monitoreo";
            case "Medication management" -> "Medicacion";
            case "Appointments" -> "Citas medicas";
            default -> valueOrPending(title);
        };
    }

    private String textForPdf(Object value) {
        var text = valueOrPending(value);
        if (text.startsWith("Clinical report generated for ")) {
            text = text
                    .replace("Clinical report generated for ", "Reporte clinico de ")
                    .replace(" from ", ", correspondiente al periodo del ")
                    .replace(" to ", " al ");
        }
        return text
                .replace("Patient: ", "Paciente: ")
                .replace(". Report type: ", ". Tipo de reporte: ")
                .replace(". Evaluation period: ", ". Periodo evaluado: ")
                .replace("VITAL_SIGNS", "Signos vitales")
                .replace("MEDICATION", "Medicacion")
                .replace("FULL_CLINICAL", "Clinico completo")
                .replace("No health monitoring observations registered for this patient in the report period.",
                        "No hay registros de monitoreo para este paciente en el periodo del reporte.")
                .replace("No health monitoring observations registered for this patient.",
                        "No hay registros de monitoreo para este paciente.")
                .replace("Latest health observation in report period recorded at ",
                        "Ultimo registro de salud en el periodo: ")
                .replace("Latest health observation recorded at ",
                        "Ultimo registro de salud: ")
                .replace(": blood pressure ", ": presion arterial ")
                .replace(", body temperature ", ", temperatura ")
                .replace(", pain level ", ", dolor ")
                .replace(", emotional state ", ", estado emocional ")
                .replace("Recent observations in report period: ", "Registros recientes del periodo: ")
                .replace("Recent observations: ", "Registros recientes: ")
                .replace(" BP ", " PA ")
                .replace(", temp ", ", temp. ")
                .replace(", pain ", ", dolor ")
                .replace(", mood ", ", animo ")
                .replace("No active clinical alerts in the report period.", "Sin alertas clinicas activas en el periodo.")
                .replace("No active clinical alerts.", "Sin alertas clinicas activas.")
                .replace("Active clinical alerts in report period: ", "Alertas clinicas activas en el periodo: ")
                .replace("Active clinical alerts: ", "Alertas clinicas activas: ")
                .replace("No active medications registered for this patient.",
                        "No hay medicacion activa registrada para este paciente.")
                .replace("Medication summary: ", "Medicacion: ")
                .replace(" active medications, ", " medicamento(s) activo(s), ")
                .replace(" low-stock medications, ", " con stock bajo, ")
                .replace(" dose administrations recorded in the report period.",
                        " dosis administrada(s) registradas en el periodo.")
                .replace("Medication summary is temporarily unavailable.",
                        "El resumen de medicacion no esta disponible temporalmente.")
                .replace("Health monitoring summary is temporarily unavailable.",
                        "El resumen de signos vitales y monitoreo no esta disponible temporalmente.")
                .replace("Appointment summary is temporarily unavailable.",
                        "El resumen de citas medicas no esta disponible temporalmente.")
                .replace("No appointments registered for this patient in the report period.",
                        "No hay citas medicas registradas para este paciente en el periodo del reporte.")
                .replace("No appointments registered for this patient.",
                        "No hay citas medicas registradas para este paciente.")
                .replace("assigned doctor", "medico asignado")
                .replace("family member", "familiar")
                .replace("No reason registered", "Sin motivo registrado")
                .replace(" appointment with ", " con ")
                .replace(" from ", " desde ")
                .replace(" to ", " hasta ")
                .replace(". Status: ", ". Estado: ")
                .replace(". Reason: ", ". Motivo: ")
                .replace("MEDICAL", "Cita medica")
                .replace("FAMILY_VISIT", "Visita familiar")
                .replace("SCHEDULED", "programada")
                .replace("CONFIRMED", "confirmada")
                .replace("COMPLETED", "completada")
                .replace("CANCELLED", "cancelada")
                .replace("CALM", "calmado")
                .replace("ANXIOUS", "ansioso")
                .replace("SAD", "triste")
                .replace("IRRITABLE", "irritable")
                .replace("CONFUSED", "confundido")
                .replace("APATHETIC", "apatico")
                .replace("MEDIUM", "media")
                .replace("HIGH", "alta");
    }
}

