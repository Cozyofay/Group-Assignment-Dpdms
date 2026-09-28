package zw.ac.uz.dpdms.report.generator;

import com.lowagie.text.Document;
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
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.dto.IncidentSummary;
import zw.ac.uz.dpdms.report.dto.ReportFilter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** PDF: landscape table of incidents plus a short count summary. */
@Component
public class PdfReportGenerator {

    private static final List<String> COLUMNS =
            List.of("Hazard", "Ref", "Occurred", "Ward", "District", "Severity", "GPS", "Summary");
    private static final float[] WIDTHS = {12f, 5f, 13f, 10f, 11f, 9f, 16f, 34f};

    public byte[] generate(List<IncidentSummary> incidents, ReportFilter filter) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 8);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

        Paragraph title = new Paragraph("Rushinga Provincial Disaster Monitoring and Management System", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph("Incident Report (approved records only)",
                FontFactory.getFont(FontFactory.HELVETICA, 11));
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(8);
        document.add(subtitle);

        Paragraph meta = new Paragraph(filter.describe() + "\nGenerated: "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                + "   |   Total incidents: " + incidents.size(), small);
        meta.setSpacingAfter(10);
        document.add(meta);

        PdfPTable table = new PdfPTable(COLUMNS.size());
        table.setWidthPercentage(100);
        try {
            table.setWidths(WIDTHS);
        } catch (Exception ignored) {
            // fall back to equal widths
        }
        for (String column : COLUMNS) {
            PdfPCell cell = new PdfPCell(new Phrase(column, headerFont));
            cell.setBackgroundColor(new Color(31, 78, 121));
            cell.setPadding(5);
            table.addCell(cell);
        }

        DateTimeFormatter when = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        boolean shaded = false;
        for (IncidentSummary incident : incidents) {
            Color background = shaded ? new Color(242, 244, 248) : Color.WHITE;
            shaded = !shaded;
            addCell(table, incident.hazard().getLabel(), normal, background);
            addCell(table, "#" + incident.id(), normal, background);
            addCell(table, incident.occurredAt() == null ? "" : incident.occurredAt().format(when), normal, background);
            addCell(table, incident.ward(), normal, background);
            addCell(table, incident.district(), normal, background);
            addCell(table, String.valueOf(incident.severity()), normal, background);
            addCell(table, incident.latitude() + ", " + incident.longitude(), small, background);
            addCell(table, incident.headline() + "\n" + ReportRows.indicators(incident), small, background);
        }
        document.add(table);

        Map<String, Integer> byHazard = new LinkedHashMap<>();
        for (HazardType hazard : HazardType.values()) {
            byHazard.put(hazard.getLabel(), 0);
        }
        incidents.forEach(incident -> byHazard.merge(incident.hazard().getLabel(), 1, Integer::sum));

        StringBuilder summary = new StringBuilder("\nIncidents by hazard:  ");
        byHazard.forEach((hazard, count) -> summary.append(hazard).append(": ").append(count).append("   "));
        document.add(new Paragraph(summary.toString(), small));

        document.close();
        return out.toByteArray();
    }

    private void addCell(PdfPTable table, String text, Font font, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setPadding(4);
        cell.setBackgroundColor(background);
        table.addCell(cell);
    }
}
