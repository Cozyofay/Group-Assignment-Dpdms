package zw.ac.uz.dpdms.mining.alert;

import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.mining.domain.MiningAccidentIncident;
import zw.ac.uz.dpdms.mining.domain.MineType;
import zw.ac.uz.dpdms.mining.domain.AccidentType;

import java.util.Optional;

/**
 * When does a Mining Accident incident deserve an alert? Kept separate from the service so it is easy to
 * unit test and easy for the Provincial Office to read.
 */
public final class MiningAccidentAlertRules {



    private MiningAccidentAlertRules() {
    }

    /** Returns the reason for alerting, or empty if this incident does not meet the criteria. */
    public static Optional<String> alertReason(MiningAccidentIncident incident) {
        if (incident.getFatalities() != null && incident.getFatalities() > 0) {
            return Optional.of(incident.getFatalities() + " fatality/fatalities reported");
        }
        if (incident.getTrappedOrInjuredMiners() != null && incident.getTrappedOrInjuredMiners() > 0) {
            return Optional.of(incident.getTrappedOrInjuredMiners() + " miners trapped or injured");
        }
        if (Boolean.TRUE.equals(incident.getRescueOngoing())) {
            return Optional.of("Rescue operations are ongoing");
        }
        if (incident.getSeverity() == Severity.HIGH || incident.getSeverity() == Severity.CRITICAL) {
            return Optional.of("Incident reported as " + incident.getSeverity() + " severity");
        }
        return Optional.empty();
    }
}
