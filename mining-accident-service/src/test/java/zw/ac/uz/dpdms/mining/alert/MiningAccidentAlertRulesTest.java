package zw.ac.uz.dpdms.mining.alert;

import org.junit.jupiter.api.Test;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.mining.domain.MiningAccidentIncident;
import zw.ac.uz.dpdms.mining.domain.MineType;
import zw.ac.uz.dpdms.mining.domain.AccidentType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiningAccidentAlertRulesTest {

    /** An incident that on its own does NOT meet any alert criterion. */
    private MiningAccidentIncident quiet() {
        MiningAccidentIncident i = new MiningAccidentIncident();
        i.setSeverity(Severity.LOW);
        i.setFatalities(0); i.setTrappedOrInjuredMiners(0); i.setRescueOngoing(false); i.setMineName("Test Mine");
        return i;
    }

    @Test
    void alertsOnFatalities() {
        MiningAccidentIncident i = quiet();
        i.setFatalities(1);
        assertTrue(MiningAccidentAlertRules.alertReason(i).isPresent());
    }

    @Test
    void alertsOnTrappedMiners() {
        MiningAccidentIncident i = quiet();
        i.setTrappedOrInjuredMiners(3);
        assertTrue(MiningAccidentAlertRules.alertReason(i).isPresent());
    }

    @Test
    void alertsOnRescueOngoing() {
        MiningAccidentIncident i = quiet();
        i.setRescueOngoing(true);
        assertTrue(MiningAccidentAlertRules.alertReason(i).isPresent());
    }

    @Test
    void highSeverityAlerts() {
        MiningAccidentIncident i = quiet();
        i.setSeverity(Severity.CRITICAL);
        assertTrue(MiningAccidentAlertRules.alertReason(i).isPresent());
    }

    @Test
    void minorIncidentDoesNotAlert() {
        MiningAccidentIncident i = quiet();
        i.setFatalities(0); i.setTrappedOrInjuredMiners(0); i.setRescueOngoing(false);
        assertFalse(MiningAccidentAlertRules.alertReason(i).isPresent());
    }
}
