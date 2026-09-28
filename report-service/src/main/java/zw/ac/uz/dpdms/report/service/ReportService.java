package zw.ac.uz.dpdms.report.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import zw.ac.uz.dpdms.common.dto.IncidentSummary;
import zw.ac.uz.dpdms.common.security.CurrentUser;
import zw.ac.uz.dpdms.report.client.HazardServiceClient;
import zw.ac.uz.dpdms.report.dto.GeneratedReport;
import zw.ac.uz.dpdms.report.dto.ReportFilter;
import zw.ac.uz.dpdms.report.dto.ReportFormat;
import zw.ac.uz.dpdms.report.generator.CsvReportGenerator;
import zw.ac.uz.dpdms.report.generator.ExcelReportGenerator;
import zw.ac.uz.dpdms.report.generator.PdfReportGenerator;
import zw.ac.uz.dpdms.report.generator.WordReportGenerator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** One place where reports are produced, in any of the four formats, for any hazard selection. */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final HazardServiceClient client;
    private final PdfReportGenerator pdf;
    private final WordReportGenerator word;
    private final ExcelReportGenerator excel;
    private final CsvReportGenerator csv;

    public ReportService(HazardServiceClient client, PdfReportGenerator pdf, WordReportGenerator word,
                         ExcelReportGenerator excel, CsvReportGenerator csv) {
        this.client = client;
        this.pdf = pdf;
        this.word = word;
        this.excel = excel;
        this.csv = csv;
    }

    public GeneratedReport generate(ReportFormat format, ReportFilter filter) {
        List<IncidentSummary> incidents = client.collect(filter);
        log.info("Building {} report with {} approved incidents for {}",
                format, incidents.size(), CurrentUser.get().username());

        byte[] content = switch (format) {
            case PDF -> pdf.generate(incidents, filter);
            case DOCX -> word.generate(incidents, filter);
            case XLSX -> excel.generate(incidents, filter);
            case CSV -> csv.generate(incidents, filter);
        };
        String fileName = "dpdms-incident-report-" + LocalDateTime.now().format(STAMP)
                + "." + format.getExtension();
        return new GeneratedReport(content, fileName, format.getContentType());
    }

    /** The same data the reports are built from, for previewing in the UI before downloading. */
    public List<IncidentSummary> preview(ReportFilter filter) {
        return client.collect(filter);
    }
}
