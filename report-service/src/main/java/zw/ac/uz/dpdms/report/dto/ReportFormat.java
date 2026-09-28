package zw.ac.uz.dpdms.report.dto;

import org.springframework.http.MediaType;

/** The four formats the brief requires. */
public enum ReportFormat {
    PDF("pdf", MediaType.APPLICATION_PDF_VALUE),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    CSV("csv", "text/csv");

    private final String extension;
    private final String contentType;

    ReportFormat(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String getExtension() { return extension; }
    public String getContentType() { return contentType; }
}
