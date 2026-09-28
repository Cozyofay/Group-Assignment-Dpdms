package zw.ac.uz.dpdms.zoonotic.alert;

import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.zoonotic.domain.ZoonoticDiseaseIncident;
import zw.ac.uz.dpdms.zoonotic.domain.EventClassification;

import java.util.Optional;

/**
 * When does a Zoonotic Disease incident deserve an alert? Kept separate from the service so it is easy to
 * unit test and easy for the Provincial Office to read.
 */
public final class ZoonoticDiseaseAlertRules {

    public static final int ANIMAL_CASE_THRESHOLD = 10;

    private ZoonoticDiseaseAlertRules() {
    }

    /** Returns the reason for alerting, or empty if this incident does not meet the criteria. */
    public static Optional<String> alertReason(ZoonoticDiseaseIncident incident) {
        if (incident.getClassification() == EventClassification.OUTBREAK) {
            return Optional.of("Event is classified as an OUTBREAK");
        }
        if (incident.getConfirmedHumanCases() != null && incident.getConfirmedHumanCases() > 0) {
            return Optional.of(incident.getConfirmedHumanCases() + " confirmed human case(s) of " + incident.getPathogenName());
        }
        if (incident.getConfirmedAnimalCases() != null && incident.getConfirmedAnimalCases() >= ANIMAL_CASE_THRESHOLD) {
            return Optional.of(incident.getConfirmedAnimalCases() + " confirmed animal cases");
        }
        if (incident.getSeverity() == Severity.HIGH || incident.getSeverity() == Severity.CRITICAL) {
            return Optional.of("Incident reported as " + incident.getSeverity() + " severity");
        }
        return Optional.empty();
    }
}
