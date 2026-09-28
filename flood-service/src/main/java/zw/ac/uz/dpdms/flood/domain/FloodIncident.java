package zw.ac.uz.dpdms.flood.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import zw.ac.uz.dpdms.common.domain.BaseIncident;
import zw.ac.uz.dpdms.flood.dto.FloodRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A flood incident: shared metadata and GPS from BaseIncident, plus the five flood indicators
 * required by the brief.
 */
@Entity
@Table(name = "flood_incident", indexes = {
        @Index(name = "idx_flood_status", columnList = "status"),
        @Index(name = "idx_flood_ward", columnList = "ward"),
        @Index(name = "idx_flood_occurred", columnList = "occurred_at")
})
public class FloodIncident extends BaseIncident {

    /** 1. Peak water level in metres. */
    @Column(name = "peak_water_level_metres", nullable = false)
    private Double peakWaterLevelMetres;

    /** 2. River basin or catchment name. */
    @Column(name = "river_basin", nullable = false, length = 120)
    private String riverBasin;

    /** 3. Number of households displaced. */
    @Column(name = "households_displaced", nullable = false)
    private Integer householdsDisplaced;

    /** 4. Estimated area flooded, in hectares. */
    @Column(name = "area_flooded_hectares", nullable = false)
    private Double areaFloodedHectares;

    /** 5. Duration of inundation, in days. */
    @Column(name = "inundation_duration_days", nullable = false)
    private Integer inundationDurationDays;

    public void applyIndicators(FloodRequest request) {
        this.peakWaterLevelMetres = request.getPeakWaterLevelMetres();
        this.riverBasin = request.getRiverBasin().trim();
        this.householdsDisplaced = request.getHouseholdsDisplaced();
        this.areaFloodedHectares = request.getAreaFloodedHectares();
        this.inundationDurationDays = request.getInundationDurationDays();
    }

    @Override
    public String headline() {
        return "Flood in " + getWard() + " (" + riverBasin + "): peak " + peakWaterLevelMetres
                + " m, " + householdsDisplaced + " households displaced";
    }

    @Override
    public Map<String, Object> indicators() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("peakWaterLevelMetres", peakWaterLevelMetres);
        values.put("riverBasin", riverBasin);
        values.put("householdsDisplaced", householdsDisplaced);
        values.put("areaFloodedHectares", areaFloodedHectares);
        values.put("inundationDurationDays", inundationDurationDays);
        return values;
    }

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
