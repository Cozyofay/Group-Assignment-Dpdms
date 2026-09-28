package zw.ac.uz.dpdms.mining.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import zw.ac.uz.dpdms.common.domain.BaseIncident;
import zw.ac.uz.dpdms.mining.dto.MiningAccidentRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A Mining Accident incident: shared metadata and GPS from BaseIncident, plus the five Mining Accident
 * indicators required by the brief.
 */
@Entity
@Table(name = "mining_accident_incident", indexes = {
        @Index(name = "idx_mining_status", columnList = "status"),
        @Index(name = "idx_mining_ward", columnList = "ward"),
        @Index(name = "idx_mining_occurred", columnList = "occurred_at")
})
public class MiningAccidentIncident extends BaseIncident {

    /** 1a. Mine name. */
    @Column(name = "mine_name", nullable = false, length = 120)
    private String mineName;

    /** 1b. Mine type: formal or artisanal. */
    @Enumerated(EnumType.STRING)
    @Column(name = "mine_type", nullable = false, length = 20)
    private MineType mineType;

    /** 2. Accident type: collapse, gas explosion, flooding or fall of ground. */
    @Enumerated(EnumType.STRING)
    @Column(name = "accident_type", nullable = false, length = 30)
    private AccidentType accidentType;

    /** 3. Number of trapped or injured miners. */
    @Column(name = "trapped_or_injured_miners", nullable = false)
    private Integer trappedOrInjuredMiners;

    /** 4. Number of fatalities. */
    @Column(name = "fatalities", nullable = false)
    private Integer fatalities;

    /** 5. Whether rescue operations are still ongoing. */
    @Column(name = "rescue_ongoing", nullable = false)
    private Boolean rescueOngoing;

    public void applyIndicators(MiningAccidentRequest request) {
        this.mineName = request.getMineName().trim();
        this.mineType = request.getMineType();
        this.accidentType = request.getAccidentType();
        this.trappedOrInjuredMiners = request.getTrappedOrInjuredMiners();
        this.fatalities = request.getFatalities();
        this.rescueOngoing = request.getRescueOngoing();
    }

    @Override
    public String headline() {
        return accidentType + " at " + mineName + " (" + mineType + ") in " + getWard() + ": "
                + trappedOrInjuredMiners + " trapped or injured, " + fatalities + " fatalities"
                + (Boolean.TRUE.equals(rescueOngoing) ? ", RESCUE ONGOING" : "");
    }

    @Override
    public Map<String, Object> indicators() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("mineName", mineName);
        values.put("mineType", mineType);
        values.put("accidentType", accidentType);
        values.put("trappedOrInjuredMiners", trappedOrInjuredMiners);
        values.put("fatalities", fatalities);
        values.put("rescueOngoing", rescueOngoing);
        return values;
    }

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
