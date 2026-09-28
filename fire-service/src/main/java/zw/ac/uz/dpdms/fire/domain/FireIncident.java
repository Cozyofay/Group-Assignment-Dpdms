package zw.ac.uz.dpdms.fire.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import zw.ac.uz.dpdms.common.domain.BaseIncident;
import zw.ac.uz.dpdms.fire.dto.FireRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A Fire incident: shared metadata and GPS from BaseIncident, plus the five Fire
 * indicators required by the brief.
 */
@Entity
@Table(name = "fire_incident", indexes = {
        @Index(name = "idx_fire_status", columnList = "status"),
        @Index(name = "idx_fire_ward", columnList = "ward"),
        @Index(name = "idx_fire_occurred", columnList = "occurred_at")
})
public class FireIncident extends BaseIncident {

    /** 1. Area burned in hectares. */
    @Column(name = "area_burned_hectares", nullable = false)
    private Double areaBurnedHectares;

    /** 2. Suspected cause: natural, accidental or deliberate. */
    @Enumerated(EnumType.STRING)
    @Column(name = "suspected_cause", nullable = false, length = 20)
    private FireCause suspectedCause;

    /** 3. Number of injuries or fatalities. */
    @Column(name = "injuries_or_fatalities", nullable = false)
    private Integer injuriesOrFatalities;

    /** 4. Number of structures destroyed. */
    @Column(name = "structures_destroyed", nullable = false)
    private Integer structuresDestroyed;

    /** 5. Whether the fire is still active (true) or contained (false). */
    @Column(name = "still_active", nullable = false)
    private Boolean stillActive;

    public void applyIndicators(FireRequest request) {
        this.areaBurnedHectares = request.getAreaBurnedHectares();
        this.suspectedCause = request.getSuspectedCause();
        this.injuriesOrFatalities = request.getInjuriesOrFatalities();
        this.structuresDestroyed = request.getStructuresDestroyed();
        this.stillActive = request.getStillActive();
    }

    @Override
    public String headline() {
        return "Fire in " + getWard() + " (" + suspectedCause + "): " + areaBurnedHectares + " ha burned, "
                + (Boolean.TRUE.equals(stillActive) ? "STILL ACTIVE" : "contained");
    }

    @Override
    public Map<String, Object> indicators() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("areaBurnedHectares", areaBurnedHectares);
        values.put("suspectedCause", suspectedCause);
        values.put("injuriesOrFatalities", injuriesOrFatalities);
        values.put("structuresDestroyed", structuresDestroyed);
        values.put("stillActive", stillActive);
        return values;
    }

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
