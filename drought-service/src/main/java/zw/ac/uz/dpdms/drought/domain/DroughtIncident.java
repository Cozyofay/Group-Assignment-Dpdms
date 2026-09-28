package zw.ac.uz.dpdms.drought.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import zw.ac.uz.dpdms.common.domain.BaseIncident;
import zw.ac.uz.dpdms.drought.dto.DroughtRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A Drought incident: shared metadata and GPS from BaseIncident, plus the five Drought
 * indicators required by the brief.
 */
@Entity
@Table(name = "drought_incident", indexes = {
        @Index(name = "idx_drought_status", columnList = "status"),
        @Index(name = "idx_drought_ward", columnList = "ward"),
        @Index(name = "idx_drought_occurred", columnList = "occurred_at")
})
public class DroughtIncident extends BaseIncident {

    /** 1. Rainfall deficit in millimetres against the seasonal norm. */
    @Column(name = "rainfall_deficit_mm", nullable = false)
    private Double rainfallDeficitMm;

    /** 2. Number of consecutive dry days. */
    @Column(name = "consecutive_dry_days", nullable = false)
    private Integer consecutiveDryDays;

    /** 3. Crop failure percentage. */
    @Column(name = "crop_failure_percentage", nullable = false)
    private Double cropFailurePercentage;

    /** 4. Number of people facing water shortages. */
    @Column(name = "people_facing_water_shortages", nullable = false)
    private Integer peopleFacingWaterShortages;

    /** 5. Livestock mortality count. */
    @Column(name = "livestock_mortality_count", nullable = false)
    private Integer livestockMortalityCount;

    public void applyIndicators(DroughtRequest request) {
        this.rainfallDeficitMm = request.getRainfallDeficitMm();
        this.consecutiveDryDays = request.getConsecutiveDryDays();
        this.cropFailurePercentage = request.getCropFailurePercentage();
        this.peopleFacingWaterShortages = request.getPeopleFacingWaterShortages();
        this.livestockMortalityCount = request.getLivestockMortalityCount();
    }

    @Override
    public String headline() {
        return "Drought in " + getWard() + ": " + rainfallDeficitMm + " mm rainfall deficit, "
                + cropFailurePercentage + "% crop failure, " + peopleFacingWaterShortages + " people short of water";
    }

    @Override
    public Map<String, Object> indicators() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("rainfallDeficitMm", rainfallDeficitMm);
        values.put("consecutiveDryDays", consecutiveDryDays);
        values.put("cropFailurePercentage", cropFailurePercentage);
        values.put("peopleFacingWaterShortages", peopleFacingWaterShortages);
        values.put("livestockMortalityCount", livestockMortalityCount);
        return values;
    }

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
