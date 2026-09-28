package zw.ac.uz.dpdms.report.dto;

/** A rendered report: the bytes, the file name and the content type for the download. */
public record GeneratedReport(byte[] content, String fileName, String contentType) {
}
