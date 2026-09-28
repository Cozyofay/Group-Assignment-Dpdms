package zw.ac.uz.dpdms.fire.dto;

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
import zw.ac.uz.dpdms.fire.domain.FireCause;

/** Shared incident metadata (ward, GPS, severity, ...) plus the five Fire indicators. */
public class FireRequest extends BaseIncidentRequest {

    @NotNull
    @DecimalMin(value = "0.0", message = "cannot be negative")
    @DecimalMax(value = "10000000.0", message = "looks unrealistic")
    private Double areaBurnedHectares;

    @NotNull
    private FireCause suspectedCause;

    @NotNull
    @Min(value = 0, message = "cannot be negative")
    @Max(value = 100000, message = "looks unrealistic")
    private Integer injuriesOrFatalities;

    @NotNull
    @Min(value = 0, message = "cannot be negative")
    @Max(value = 100000, message = "looks unrealistic")
    private Integer structuresDestroyed;

    @NotNull
    private Boolean stillActive;

    public Double getAreaBurnedHectares() { return areaBurnedHectares; }
    public void setAreaBurnedHectares(Double areaBurnedHectares) { this.areaBurnedHectares = areaBurnedHectares; }

    public FireCause getSuspectedCause() { return suspectedCause; }
    public void setSuspectedCause(FireCause suspectedCause) { this.suspectedCause = suspectedCause; }

    public Integer getInjuriesOrFatalities() { return injuriesOrFatalities; }
    public void setInjuriesOrFatalities(Integer injuriesOrFatalities) { this.injuriesOrFatalities = injuriesOrFatalities; }

    public Integer getStructuresDestroyed() { return structuresDestroyed; }
    public void setStructuresDestroyed(Integer structuresDestroyed) { this.structuresDestroyed = structuresDestroyed; }

    public Boolean getStillActive() { return stillActive; }
    public void setStillActive(Boolean stillActive) { this.stillActive = stillActive; }
}
