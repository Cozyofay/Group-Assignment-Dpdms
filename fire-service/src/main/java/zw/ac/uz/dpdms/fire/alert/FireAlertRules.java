package zw.ac.uz.dpdms.fire.alert;

import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.fire.domain.FireIncident;
import zw.ac.uz.dpdms.fire.domain.FireCause;

import java.util.Optional;

/**
 * When does a Fire incident deserve an alert? Kept separate from the service so it is easy to
 * unit test and easy for the Provincial Office to read.
 */
public final class FireAlertRules {

    public static final double BURNED_AREA_THRESHOLD_HECTARES = 50.0;

    private FireAlertRules() {
    }

    /** Returns the reason for alerting, or empty if this incident does not meet the criteria. */
    public static Optional<String> alertReason(FireIncident incident) {
        if (Boolean.TRUE.equals(incident.getStillActive())) {
            return Optional.of("The fire is still burning");
        }
        if (incident.getInjuriesOrFatalities() != null && incident.getInjuriesOrFatalities() > 0) {
            return Optional.of(incident.getInjuriesOrFatalities() + " injuries or fatalities reported");
        }
        if (incident.getAreaBurnedHectares() != null && incident.getAreaBurnedHectares() >= BURNED_AREA_THRESHOLD_HECTARES) {
            return Optional.of(incident.getAreaBurnedHectares() + " hectares burned");
        }
        if (incident.getSeverity() == Severity.HIGH || incident.getSeverity() == Severity.CRITICAL) {
            return Optional.of("Incident reported as " + incident.getSeverity() + " severity");
        }
        return Optional.empty();
    }
}
