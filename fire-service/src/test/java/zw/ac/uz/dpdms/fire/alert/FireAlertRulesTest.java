package zw.ac.uz.dpdms.fire.alert;

import org.junit.jupiter.api.Test;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.fire.domain.FireIncident;
import zw.ac.uz.dpdms.fire.domain.FireCause;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FireAlertRulesTest {

    /** An incident that on its own does NOT meet any alert criterion. */
    private FireIncident quiet() {
        FireIncident i = new FireIncident();
        i.setSeverity(Severity.LOW);
        i.setStillActive(false); i.setInjuriesOrFatalities(0); i.setAreaBurnedHectares(1.0);
        return i;
    }

    @Test
    void alertsOnStillBurning() {
        FireIncident i = quiet();
        i.setStillActive(true);
        assertTrue(FireAlertRules.alertReason(i).isPresent());
    }

    @Test
    void alertsOnCasualties() {
        FireIncident i = quiet();
        i.setInjuriesOrFatalities(2);
        assertTrue(FireAlertRules.alertReason(i).isPresent());
    }

    @Test
    void alertsOnLargeArea() {
        FireIncident i = quiet();
        i.setAreaBurnedHectares(120.0);
        assertTrue(FireAlertRules.alertReason(i).isPresent());
    }

    @Test
    void highSeverityAlerts() {
        FireIncident i = quiet();
        i.setSeverity(Severity.CRITICAL);
        assertTrue(FireAlertRules.alertReason(i).isPresent());
    }

    @Test
    void minorIncidentDoesNotAlert() {
        FireIncident i = quiet();
        i.setStillActive(false); i.setInjuriesOrFatalities(0); i.setAreaBurnedHectares(3.0);
        assertFalse(FireAlertRules.alertReason(i).isPresent());
    }
}
