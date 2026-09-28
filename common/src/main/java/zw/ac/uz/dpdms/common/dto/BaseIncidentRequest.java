package zw.ac.uz.dpdms.common.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import zw.ac.uz.dpdms.common.domain.Severity;

import java.time.LocalDateTime;

/**
 * Shared metadata every hazard create/update request must carry. Each hazard's request class
 * (FloodRequest, DroughtRequest, ...) extends this and adds its five indicators.
 * GPS bounds cover Zimbabwe; widen them if the system is rolled out beyond the country.
 */
public abstract class BaseIncidentRequest {

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = ValidationPatterns.SAFE_TEXT, message = ValidationPatterns.SAFE_TEXT_MESSAGE)
    private String ward;

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = ValidationPatterns.SAFE_TEXT, message = ValidationPatterns.SAFE_TEXT_MESSAGE)
    private String district;

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = ValidationPatterns.SAFE_TEXT, message = ValidationPatterns.SAFE_TEXT_MESSAGE)
    private String province;

    @NotNull
    @PastOrPresent(message = "cannot be in the future")
    private LocalDateTime occurredAt;

    @NotNull
    private Severity severity;

    @NotNull
    @DecimalMin(value = "-22.5", message = "must be within Zimbabwe (-22.5 to -15.5)")
    @DecimalMax(value = "-15.5", message = "must be within Zimbabwe (-22.5 to -15.5)")
    private Double latitude;

    @NotNull
    @DecimalMin(value = "25.0", message = "must be within Zimbabwe (25.0 to 33.1)")
    @DecimalMax(value = "33.1", message = "must be within Zimbabwe (25.0 to 33.1)")
    private Double longitude;

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
