package zw.ac.uz.dpdms.mining.dto;

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
import zw.ac.uz.dpdms.mining.domain.MineType;
import zw.ac.uz.dpdms.mining.domain.AccidentType;

/** Shared incident metadata (ward, GPS, severity, ...) plus the five Mining Accident indicators. */
public class MiningAccidentRequest extends BaseIncidentRequest {

    @NotBlank
    @Size(max = 120)
    @Pattern(regexp = ValidationPatterns.SAFE_TEXT, message = ValidationPatterns.SAFE_TEXT_MESSAGE)
    private String mineName;

    @NotNull
    private MineType mineType;

    @NotNull
    private AccidentType accidentType;

    @NotNull
    @Min(value = 0, message = "cannot be negative")
    @Max(value = 100000, message = "looks unrealistic")
    private Integer trappedOrInjuredMiners;

    @NotNull
    @Min(value = 0, message = "cannot be negative")
    @Max(value = 100000, message = "looks unrealistic")
    private Integer fatalities;

    @NotNull
    private Boolean rescueOngoing;

    public String getMineName() { return mineName; }
    public void setMineName(String mineName) { this.mineName = mineName; }

    public MineType getMineType() { return mineType; }
    public void setMineType(MineType mineType) { this.mineType = mineType; }

    public AccidentType getAccidentType() { return accidentType; }
    public void setAccidentType(AccidentType accidentType) { this.accidentType = accidentType; }

    public Integer getTrappedOrInjuredMiners() { return trappedOrInjuredMiners; }
    public void setTrappedOrInjuredMiners(Integer trappedOrInjuredMiners) { this.trappedOrInjuredMiners = trappedOrInjuredMiners; }

    public Integer getFatalities() { return fatalities; }
    public void setFatalities(Integer fatalities) { this.fatalities = fatalities; }

    public Boolean getRescueOngoing() { return rescueOngoing; }
    public void setRescueOngoing(Boolean rescueOngoing) { this.rescueOngoing = rescueOngoing; }
}
