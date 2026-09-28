package zw.ac.uz.dpdms.report.generator;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;
import zw.ac.uz.dpdms.common.dto.IncidentSummary;
import zw.ac.uz.dpdms.report.dto.ReportFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** DOCX: a short narrative report with one section per incident. */
@Component
public class WordReportGenerator {

    private static final List<String> COLUMNS = List.of("Hazard", "Ref", "Occurred", "Ward", "Severity", "Summary");

    public byte[] generate(List<IncidentSummary> incidents, ReportFilter filter) {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XWPFParagraph title = document.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText("Rushinga Provincial Disaster Monitoring and Management System");
            titleRun.setBold(true);
            titleRun.setFontSize(15);
            titleRun.addBreak();
            XWPFRun subtitleRun = title.createRun();
            subtitleRun.setText("Incident Report (approved records only)");
            subtitleRun.setFontSize(12);

            XWPFParagraph meta = document.createParagraph();
            XWPFRun metaRun = meta.createRun();
            metaRun.setText(filter.describe());
            metaRun.addBreak();
            metaRun.setText("Generated: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            metaRun.addBreak();
            metaRun.setText("Total incidents: " + incidents.size());

            XWPFTable table = document.createTable(1, COLUMNS.size());
            XWPFTableRow header = table.getRow(0);
            for (int i = 0; i < COLUMNS.size(); i++) {
                XWPFRun run = header.getCell(i).addParagraph().createRun();
                run.setText(COLUMNS.get(i));
                run.setBold(true);
            }

            DateTimeFormatter when = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (IncidentSummary incident : incidents) {
                XWPFTableRow row = table.createRow();
                row.getCell(0).setText(incident.hazard().getLabel());
                row.getCell(1).setText(String.valueOf(incident.id()));
                row.getCell(2).setText(incident.occurredAt() == null ? "" : incident.occurredAt().format(when));
                row.getCell(3).setText(incident.ward());
                row.getCell(4).setText(String.valueOf(incident.severity()));
                row.getCell(5).setText(incident.headline());
            }

            XWPFParagraph detailHeading = document.createParagraph();
            XWPFRun detailRun = detailHeading.createRun();
            detailRun.addBreak();
            detailRun.setText("Detailed indicators");
            detailRun.setBold(true);
            detailRun.setFontSize(13);

            for (IncidentSummary incident : incidents) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(incident.hazard().getLabel() + " #" + incident.id() + " - " + incident.ward()
                        + " (" + incident.district() + ")");
                run.setBold(true);
                run.addBreak();
                run.setText("GPS: " + incident.latitude() + ", " + incident.longitude()
                        + "   |   Reported by: " + incident.reporterUsername());
                run.addBreak();
                run.setText(ReportRows.indicators(incident));
            }

            document.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not build the Word report", ex);
        }
    }
}
