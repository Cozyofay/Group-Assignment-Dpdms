package zw.ac.uz.dpdms.flood.alert;

import org.junit.jupiter.api.Test;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.flood.domain.FloodIncident;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FloodAlertRulesTest {

    private FloodIncident incident(double level, int households, Severity severity) {
        FloodIncident incident = new FloodIncident();
        incident.setPeakWaterLevelMetres(level);
        incident.setHouseholdsDisplaced(households);
        incident.setSeverity(severity);
        return incident;
    }

    @Test
    void waterAboveDangerLevelAlerts() {
        assertTrue(FloodAlertRules.alertReason(incident(2.0, 0, Severity.LOW)).isPresent());
        assertTrue(FloodAlertRules.alertReason(incident(3.5, 0, Severity.LOW)).isPresent());
    }

    @Test
    void massDisplacementAlerts() {
        assertTrue(FloodAlertRules.alertReason(incident(0.4, 50, Severity.LOW)).isPresent());
    }

    @Test
    void highSeverityAlerts() {
        assertTrue(FloodAlertRules.alertReason(incident(0.2, 1, Severity.HIGH)).isPresent());
        assertTrue(FloodAlertRules.alertReason(incident(0.2, 1, Severity.CRITICAL)).isPresent());
    }

    @Test
    void minorFloodDoesNotAlert() {
        assertFalse(FloodAlertRules.alertReason(incident(0.5, 3, Severity.LOW)).isPresent());
        assertFalse(FloodAlertRules.alertReason(incident(1.9, 49, Severity.MODERATE)).isPresent());
    }
}
