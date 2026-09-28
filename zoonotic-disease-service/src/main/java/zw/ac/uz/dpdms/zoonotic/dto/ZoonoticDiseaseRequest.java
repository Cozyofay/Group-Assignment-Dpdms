package zw.ac.uz.dpdms.zoonotic.dto;

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
import zw.ac.uz.dpdms.zoonotic.domain.EventClassification;

/** Shared incident metadata (ward, GPS, severity, ...) plus the five Zoonotic Disease indicators. */
public class ZoonoticDiseaseRequest extends BaseIncidentRequest {

    @NotBlank
    @Size(max = 120)
    @Pattern(regexp = ValidationPatterns.SAFE_TEXT, message = ValidationPatterns.SAFE_TEXT_MESSAGE)
    private String pathogenName;

    @NotBlank
    @Size(max = 120)
    @Pattern(regexp = ValidationPatterns.SAFE_TEXT, message = ValidationPatterns.SAFE_TEXT_MESSAGE)
    private String animalSpeciesAffected;

    @NotNull
    @Min(value = 0, message = "cannot be negative")
    @Max(value = 1000000, message = "looks unrealistic")
    private Integer confirmedHumanCases;

    @NotNull
    @Min(value = 0, message = "cannot be negative")
    @Max(value = 1000000, message = "looks unrealistic")
    private Integer confirmedAnimalCases;

    @NotNull
    private EventClassification classification;

    public String getPathogenName() { return pathogenName; }
    public void setPathogenName(String pathogenName) { this.pathogenName = pathogenName; }

    public String getAnimalSpeciesAffected() { return animalSpeciesAffected; }
    public void setAnimalSpeciesAffected(String animalSpeciesAffected) { this.animalSpeciesAffected = animalSpeciesAffected; }

    public Integer getConfirmedHumanCases() { return confirmedHumanCases; }
    public void setConfirmedHumanCases(Integer confirmedHumanCases) { this.confirmedHumanCases = confirmedHumanCases; }

    public Integer getConfirmedAnimalCases() { return confirmedAnimalCases; }
    public void setConfirmedAnimalCases(Integer confirmedAnimalCases) { this.confirmedAnimalCases = confirmedAnimalCases; }

    public EventClassification getClassification() { return classification; }
    public void setClassification(EventClassification classification) { this.classification = classification; }
}
