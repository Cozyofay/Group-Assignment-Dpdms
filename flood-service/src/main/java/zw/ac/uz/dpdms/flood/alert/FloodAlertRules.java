package zw.ac.uz.dpdms.flood.alert;

import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.flood.domain.FloodIncident;

import java.util.Optional;

/**
 * When does a flood incident deserve an alert? Kept separate from the service so it is easy to
 * unit test and easy for the marker (or the Provincial Office) to read.
 */
public final class FloodAlertRules {

    public static final double DANGER_LEVEL_METRES = 2.0;
    public static final int DISPLACED_HOUSEHOLDS_THRESHOLD = 50;

    private FloodAlertRules() {
    }

    /** Returns the reason for alerting, or empty if this incident does not meet the criteria. */
    public static Optional<String> alertReason(FloodIncident incident) {
        if (incident.getPeakWaterLevelMetres() != null
                && incident.getPeakWaterLevelMetres() >= DANGER_LEVEL_METRES) {
            return Optional.of("Peak water level of " + incident.getPeakWaterLevelMetres()
                    + " m is at or above the " + DANGER_LEVEL_METRES + " m danger threshold");
        }
        if (incident.getHouseholdsDisplaced() != null
                && incident.getHouseholdsDisplaced() >= DISPLACED_HOUSEHOLDS_THRESHOLD) {
            return Optional.of(incident.getHouseholdsDisplaced() + " households displaced");
        }
        if (incident.getSeverity() == Severity.HIGH || incident.getSeverity() == Severity.CRITICAL) {
            return Optional.of("Incident reported as " + incident.getSeverity() + " severity");
        }
        return Optional.empty();
    }
}
