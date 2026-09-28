package zw.ac.uz.dpdms.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.IncidentStatus;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.common.dto.IncidentSummary;
import zw.ac.uz.dpdms.report.dto.ReportFilter;
import zw.ac.uz.dpdms.report.generator.CsvReportGenerator;
import zw.ac.uz.dpdms.report.generator.ExcelReportGenerator;
import zw.ac.uz.dpdms.report.generator.PdfReportGenerator;
import zw.ac.uz.dpdms.report.generator.ReportRows;
import zw.ac.uz.dpdms.report.generator.WordReportGenerator;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The four formats must all render, and must all contain the same incident data. */
class ReportGeneratorTest {

    private final ReportFilter filter = new ReportFilter(null, null, null, null, null, null);

    private List<IncidentSummary> incidents() {
        Map<String, Object> floodIndicators = new LinkedHashMap<>();
        floodIndicators.put("peakWaterLevelMetres", 2.8);
        floodIndicators.put("riverBasin", "Mazowe");
        Map<String, Object> miningIndicators = new LinkedHashMap<>();
        miningIndicators.put("mineName", "Shamva Gold Mine");
        miningIndicators.put("fatalities", 1);

        return List.of(
                new IncidentSummary(HazardType.FLOOD, 1L, "Ward 1", "Rushinga", "Mashonaland Central",
                        LocalDateTime.of(2026, 1, 14, 6, 30), Severity.HIGH, IncidentStatus.APPROVED,
                        -16.6231, 32.0854, "recorder.flood.ward1", "Flood in Ward 1 (Mazowe)", floodIndicators),
                new IncidentSummary(HazardType.MINING_ACCIDENT, 4L, "Ward 2", "Rushinga", "Mashonaland Central",
                        LocalDateTime.of(2026, 2, 3, 11, 0), Severity.CRITICAL, IncidentStatus.APPROVED,
                        -16.54, 32.15, "recorder.mining.ward1", "Collapse at Shamva Gold Mine", miningIndicators));
    }

    @Test
    @DisplayName("CSV contains a header and one line per incident")
    void csvReport() {
        String csv = new String(new CsvReportGenerator().generate(incidents(), filter), StandardCharsets.UTF_8);
        assertTrue(csv.contains("Hazard,Ref,Occurred"));
        assertTrue(csv.contains("Mazowe"));
        assertTrue(csv.contains("Shamva Gold Mine"));
        assertEquals(3, csv.strip().split("\\R").length, "header plus two incidents");
    }

    @Test
    @DisplayName("XLSX is a real Excel file (PK zip header) and is not empty")
    void excelReport() {
        byte[] xlsx = new ExcelReportGenerator().generate(incidents(), filter);
        assertTrue(xlsx.length > 1000);
        assertEquals('P', (char) xlsx[0]);
        assertEquals('K', (char) xlsx[1]);
    }

    @Test
    @DisplayName("DOCX is a real Word file and is not empty")
    void wordReport() {
        byte[] docx = new WordReportGenerator().generate(incidents(), filter);
        assertTrue(docx.length > 1000);
        assertEquals('P', (char) docx[0]);
        assertEquals('K', (char) docx[1]);
    }

    @Test
    @DisplayName("PDF starts with the %PDF- signature")
    void pdfReport() {
        byte[] pdf = new PdfReportGenerator().generate(incidents(), filter);
        assertTrue(pdf.length > 500);
        assertEquals("%PDF-", new String(pdf, 0, 5, StandardCharsets.ISO_8859_1));
    }

    @Test
    void emptyResultStillProducesValidFiles() {
        assertTrue(new PdfReportGenerator().generate(List.of(), filter).length > 300);
        assertTrue(new ExcelReportGenerator().generate(List.of(), filter).length > 500);
        assertTrue(new WordReportGenerator().generate(List.of(), filter).length > 500);
    }

    @Test
    void indicatorNamesAreHumanReadable() {
        assertEquals("Peak water level metres", ReportRows.readable("peakWaterLevelMetres"));
        assertEquals("Mine name", ReportRows.readable("mineName"));
    }
}
