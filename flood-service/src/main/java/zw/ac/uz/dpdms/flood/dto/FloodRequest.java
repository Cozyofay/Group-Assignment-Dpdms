package zw.ac.uz.dpdms.flood.dto;

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

/** Shared incident metadata (ward, GPS, severity, ...) plus the five flood indicators. */
public class FloodRequest extends BaseIncidentRequest {

    @NotNull
    @DecimalMin(value = "0.0", message = "cannot be negative")
    @DecimalMax(value = "50.0", message = "looks unrealistic (max 50 m)")
    private Double peakWaterLevelMetres;

    @NotBlank
    @Size(max = 120)
    @Pattern(regexp = ValidationPatterns.SAFE_TEXT, message = ValidationPatterns.SAFE_TEXT_MESSAGE)
    private String riverBasin;

    @NotNull
    @Min(value = 0, message = "cannot be negative")
    @Max(value = 1000000, message = "looks unrealistic")
    private Integer householdsDisplaced;

    @NotNull
    @DecimalMin(value = "0.0", message = "cannot be negative")
    private Double areaFloodedHectares;

    @NotNull
    @Min(value = 0, message = "cannot be negative")
    @Max(value = 3650, message = "looks unrealistic (max 10 years)")
    private Integer inundationDurationDays;

    public Double getPeakWaterLevelMetres() { return peakWaterLevelMetres; }
    public void setPeakWaterLevelMetres(Double peakWaterLevelMetres) { this.peakWaterLevelMetres = peakWaterLevelMetres; }

    public String getRiverBasin() { return riverBasin; }
    public void setRiverBasin(String riverBasin) { this.riverBasin = riverBasin; }

    public Integer getHouseholdsDisplaced() { return householdsDisplaced; }
    public void setHouseholdsDisplaced(Integer householdsDisplaced) { this.householdsDisplaced = householdsDisplaced; }

    public Double getAreaFloodedHectares() { return areaFloodedHectares; }
    public void setAreaFloodedHectares(Double areaFloodedHectares) { this.areaFloodedHectares = areaFloodedHectares; }

    public Integer getInundationDurationDays() { return inundationDurationDays; }
    public void setInundationDurationDays(Integer inundationDurationDays) { this.inundationDurationDays = inundationDurationDays; }
}
