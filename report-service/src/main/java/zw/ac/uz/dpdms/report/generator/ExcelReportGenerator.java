package zw.ac.uz.dpdms.report.generator;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.dto.IncidentSummary;
import zw.ac.uz.dpdms.report.dto.ReportFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** XLSX with one sheet of incidents and one summary sheet of counts. */
@Component
public class ExcelReportGenerator {

    public byte[] generate(List<IncidentSummary> incidents, ReportFilter filter) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeIncidentSheet(workbook, incidents, filter);
            writeSummarySheet(workbook, incidents);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not build the Excel report", ex);
        }
    }

    private void writeIncidentSheet(Workbook workbook, List<IncidentSummary> incidents, ReportFilter filter) {
        Sheet sheet = workbook.createSheet("Incidents");
        CellStyle titleStyle = boldStyle(workbook, 14);
        CellStyle headerStyle = headerStyle(workbook);

        Row title = sheet.createRow(0);
        title.createCell(0).setCellValue("DPDMS Incident Report");
        title.getCell(0).setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, ReportRows.HEADERS.size() - 1));

        Row subtitle = sheet.createRow(1);
        subtitle.createCell(0).setCellValue(filter.describe()
                + "   |   Approved records only   |   Generated "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, ReportRows.HEADERS.size() - 1));

        Row header = sheet.createRow(3);
        for (int i = 0; i < ReportRows.HEADERS.size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(ReportRows.HEADERS.get(i));
            cell.setCellStyle(headerStyle);
        }

        int rowIndex = 4;
        for (IncidentSummary incident : incidents) {
            Row row = sheet.createRow(rowIndex++);
            List<String> values = ReportRows.toRow(incident);
            for (int i = 0; i < values.size(); i++) {
                row.createCell(i).setCellValue(values.get(i));
            }
        }
        for (int i = 0; i < ReportRows.HEADERS.size(); i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) > 12000) {
                sheet.setColumnWidth(i, 12000);
            }
        }
        sheet.createFreezePane(0, 4);
    }

    private void writeSummarySheet(Workbook workbook, List<IncidentSummary> incidents) {
        Sheet sheet = workbook.createSheet("Summary");
        CellStyle headerStyle = headerStyle(workbook);

        Map<String, Integer> byHazard = new LinkedHashMap<>();
        for (HazardType hazard : HazardType.values()) {
            byHazard.put(hazard.getLabel(), 0);
        }
        Map<String, Integer> bySeverity = new LinkedHashMap<>();
        for (IncidentSummary incident : incidents) {
            byHazard.merge(incident.hazard().getLabel(), 1, Integer::sum);
            bySeverity.merge(String.valueOf(incident.severity()), 1, Integer::sum);
        }

        int rowIndex = 0;
        Row header = sheet.createRow(rowIndex++);
        header.createCell(0).setCellValue("Incidents by hazard");
        header.createCell(1).setCellValue("Count");
        header.getCell(0).setCellStyle(headerStyle);
        header.getCell(1).setCellStyle(headerStyle);
        for (Map.Entry<String, Integer> entry : byHazard.entrySet()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }

        rowIndex++;
        Row severityHeader = sheet.createRow(rowIndex++);
        severityHeader.createCell(0).setCellValue("Incidents by severity");
        severityHeader.createCell(1).setCellValue("Count");
        severityHeader.getCell(0).setCellStyle(headerStyle);
        severityHeader.getCell(1).setCellStyle(headerStyle);
        for (Map.Entry<String, Integer> entry : bySeverity.entrySet()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }

        Row total = sheet.createRow(rowIndex + 1);
        total.createCell(0).setCellValue("Total approved incidents");
        total.createCell(1).setCellValue(incidents.size());
        total.getCell(0).setCellStyle(headerStyle);

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private CellStyle boldStyle(Workbook workbook, int points) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) points);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = boldStyle(workbook, 11);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }
}
