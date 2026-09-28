package zw.ac.uz.dpdms.zoonotic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import zw.ac.uz.dpdms.common.domain.BaseIncident;
import zw.ac.uz.dpdms.zoonotic.dto.ZoonoticDiseaseRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A Zoonotic Disease incident: shared metadata and GPS from BaseIncident, plus the five Zoonotic Disease
 * indicators required by the brief.
 */
@Entity
@Table(name = "zoonotic_disease_incident", indexes = {
        @Index(name = "idx_zoonotic_status", columnList = "status"),
        @Index(name = "idx_zoonotic_ward", columnList = "ward"),
        @Index(name = "idx_zoonotic_occurred", columnList = "occurred_at")
})
public class ZoonoticDiseaseIncident extends BaseIncident {

    /** 1. Pathogen or disease name, e.g. anthrax, rabies or brucellosis. */
    @Column(name = "pathogen_name", nullable = false, length = 120)
    private String pathogenName;

    /** 2. Animal species affected. */
    @Column(name = "animal_species_affected", nullable = false, length = 120)
    private String animalSpeciesAffected;

    /** 3. Number of confirmed human cases. */
    @Column(name = "confirmed_human_cases", nullable = false)
    private Integer confirmedHumanCases;

    /** 4. Number of confirmed animal cases. */
    @Column(name = "confirmed_animal_cases", nullable = false)
    private Integer confirmedAnimalCases;

    /** 5. Whether the event is a cluster or a full outbreak. */
    @Enumerated(EnumType.STRING)
    @Column(name = "classification", nullable = false, length = 20)
    private EventClassification classification;

    public void applyIndicators(ZoonoticDiseaseRequest request) {
        this.pathogenName = request.getPathogenName().trim();
        this.animalSpeciesAffected = request.getAnimalSpeciesAffected().trim();
        this.confirmedHumanCases = request.getConfirmedHumanCases();
        this.confirmedAnimalCases = request.getConfirmedAnimalCases();
        this.classification = request.getClassification();
    }

    @Override
    public String headline() {
        return pathogenName + " (" + animalSpeciesAffected + ") in " + getWard() + ": "
                + confirmedHumanCases + " human and " + confirmedAnimalCases + " animal cases, " + classification;
    }

    @Override
    public Map<String, Object> indicators() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("pathogenName", pathogenName);
        values.put("animalSpeciesAffected", animalSpeciesAffected);
        values.put("confirmedHumanCases", confirmedHumanCases);
        values.put("confirmedAnimalCases", confirmedAnimalCases);
        values.put("classification", classification);
        return values;
    }

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
