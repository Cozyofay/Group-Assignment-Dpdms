package zw.ac.uz.dpdms.drought.alert;

import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.drought.domain.DroughtIncident;


import java.util.Optional;

/**
 * When does a Drought incident deserve an alert? Kept separate from the service so it is easy to
 * unit test and easy for the Provincial Office to read.
 */
public final class DroughtAlertRules {

    public static final double CROP_FAILURE_THRESHOLD = 50.0;
    public static final int WATER_SHORTAGE_THRESHOLD = 1000;
    public static final int DRY_DAYS_THRESHOLD = 60;

    private DroughtAlertRules() {
    }

    /** Returns the reason for alerting, or empty if this incident does not meet the criteria. */
    public static Optional<String> alertReason(DroughtIncident incident) {
        if (incident.getCropFailurePercentage() != null && incident.getCropFailurePercentage() >= CROP_FAILURE_THRESHOLD) {
            return Optional.of("Crop failure of " + incident.getCropFailurePercentage() + "% is at or above the " + CROP_FAILURE_THRESHOLD + "% threshold");
        }
        if (incident.getPeopleFacingWaterShortages() != null && incident.getPeopleFacingWaterShortages() >= WATER_SHORTAGE_THRESHOLD) {
            return Optional.of(incident.getPeopleFacingWaterShortages() + " people are facing water shortages");
        }
        if (incident.getConsecutiveDryDays() != null && incident.getConsecutiveDryDays() >= DRY_DAYS_THRESHOLD) {
            return Optional.of(incident.getConsecutiveDryDays() + " consecutive dry days");
        }
        if (incident.getSeverity() == Severity.HIGH || incident.getSeverity() == Severity.CRITICAL) {
            return Optional.of("Incident reported as " + incident.getSeverity() + " severity");
        }
        return Optional.empty();
    }
}
