package zw.ac.uz.dpdms.drought.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import zw.ac.uz.dpdms.common.dto.BaseIncidentRequest;
import zw.ac.uz.dpdms.common.dto.ValidationPatterns;


/** Shared incident metadata (ward, GPS, severity, ...) plus the five Drought indicators. */
public class DroughtRequest extends BaseIncidentRequest {

    @NotNull
    @DecimalMin(value = "0.0", message = "cannot be negative")
    @DecimalMax(value = "5000.0", message = "looks unrealistic")
    private Double rainfallDeficitMm;

    @NotNull
    @Min(value = 0, message = "cannot be negative")
    @Max(value = 3650, message = "looks unrealistic (max 10 years)")
    private Integer consecutiveDryDays;

    @NotNull
    @DecimalMin(value = "0.0", message = "cannot be negative")
    @DecimalMax(value = "100.0", message = "cannot exceed 100%")
    private Double cropFailurePercentage;

    @NotNull
    @Min(value = 0, message = "cannot be negative")
    @Max(value = 10000000, message = "looks unrealistic")
    private Integer peopleFacingWaterShortages;

    @NotNull
    @Min(value = 0, message = "cannot be negative")
    @Max(value = 10000000, message = "looks unrealistic")
    private Integer livestockMortalityCount;

    public Double getRainfallDeficitMm() { return rainfallDeficitMm; }
    public void setRainfallDeficitMm(Double rainfallDeficitMm) { this.rainfallDeficitMm = rainfallDeficitMm; }

    public Integer getConsecutiveDryDays() { return consecutiveDryDays; }
    public void setConsecutiveDryDays(Integer consecutiveDryDays) { this.consecutiveDryDays = consecutiveDryDays; }

    public Double getCropFailurePercentage() { return cropFailurePercentage; }
    public void setCropFailurePercentage(Double cropFailurePercentage) { this.cropFailurePercentage = cropFailurePercentage; }

    public Integer getPeopleFacingWaterShortages() { return peopleFacingWaterShortages; }
    public void setPeopleFacingWaterShortages(Integer peopleFacingWaterShortages) { this.peopleFacingWaterShortages = peopleFacingWaterShortages; }

    public Integer getLivestockMortalityCount() { return livestockMortalityCount; }
    public void setLivestockMortalityCount(Integer livestockMortalityCount) { this.livestockMortalityCount = livestockMortalityCount; }
}
