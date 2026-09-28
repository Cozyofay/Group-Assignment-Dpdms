package zw.ac.uz.dpdms.drought.alert;

import org.junit.jupiter.api.Test;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.drought.domain.DroughtIncident;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DroughtAlertRulesTest {

    /** An incident that on its own does NOT meet any alert criterion. */
    private DroughtIncident quiet() {
        DroughtIncident i = new DroughtIncident();
        i.setSeverity(Severity.LOW);
        i.setCropFailurePercentage(1.0); i.setPeopleFacingWaterShortages(1); i.setConsecutiveDryDays(1);
        return i;
    }

    @Test
    void alertsOnHighCropFailure() {
        DroughtIncident i = quiet();
        i.setCropFailurePercentage(62.0);
        assertTrue(DroughtAlertRules.alertReason(i).isPresent());
    }

    @Test
    void alertsOnWaterShortage() {
        DroughtIncident i = quiet();
        i.setPeopleFacingWaterShortages(1500);
        assertTrue(DroughtAlertRules.alertReason(i).isPresent());
    }

    @Test
    void alertsOnDryDays() {
        DroughtIncident i = quiet();
        i.setConsecutiveDryDays(75);
        assertTrue(DroughtAlertRules.alertReason(i).isPresent());
    }

    @Test
    void highSeverityAlerts() {
        DroughtIncident i = quiet();
        i.setSeverity(Severity.CRITICAL);
        assertTrue(DroughtAlertRules.alertReason(i).isPresent());
    }

    @Test
    void minorIncidentDoesNotAlert() {
        DroughtIncident i = quiet();
        i.setCropFailurePercentage(10.0); i.setPeopleFacingWaterShortages(20); i.setConsecutiveDryDays(5);
        assertFalse(DroughtAlertRules.alertReason(i).isPresent());
    }
}
