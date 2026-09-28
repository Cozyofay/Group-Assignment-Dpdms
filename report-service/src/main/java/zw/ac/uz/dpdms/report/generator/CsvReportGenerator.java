package zw.ac.uz.dpdms.report.generator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;
import zw.ac.uz.dpdms.common.dto.IncidentSummary;
import zw.ac.uz.dpdms.report.dto.ReportFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class CsvReportGenerator {

    public byte[] generate(List<IncidentSummary> incidents, ReportFilter filter) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(ReportRows.HEADERS.toArray(String[]::new))
                .build();
        try (CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(out, StandardCharsets.UTF_8), format)) {
            for (IncidentSummary incident : incidents) {
                printer.printRecord(ReportRows.toRow(incident));
            }
            printer.flush();
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not build the CSV report", ex);
        }
        return out.toByteArray();
    }
}
