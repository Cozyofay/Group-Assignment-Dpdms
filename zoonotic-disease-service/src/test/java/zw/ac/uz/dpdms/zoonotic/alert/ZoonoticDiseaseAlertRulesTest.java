package zw.ac.uz.dpdms.zoonotic.alert;

import org.junit.jupiter.api.Test;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.zoonotic.domain.ZoonoticDiseaseIncident;
import zw.ac.uz.dpdms.zoonotic.domain.EventClassification;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoonoticDiseaseAlertRulesTest {

    /** An incident that on its own does NOT meet any alert criterion. */
    private ZoonoticDiseaseIncident quiet() {
        ZoonoticDiseaseIncident i = new ZoonoticDiseaseIncident();
        i.setSeverity(Severity.LOW);
        i.setClassification(EventClassification.CLUSTER); i.setConfirmedHumanCases(0); i.setConfirmedAnimalCases(0); i.setPathogenName("Anthrax");
        return i;
    }

    @Test
    void alertsOnOutbreak() {
        ZoonoticDiseaseIncident i = quiet();
        i.setClassification(EventClassification.OUTBREAK);
        assertTrue(ZoonoticDiseaseAlertRules.alertReason(i).isPresent());
    }

    @Test
    void alertsOnHumanCases() {
        ZoonoticDiseaseIncident i = quiet();
        i.setConfirmedHumanCases(1);
        assertTrue(ZoonoticDiseaseAlertRules.alertReason(i).isPresent());
    }

    @Test
    void alertsOnAnimalCases() {
        ZoonoticDiseaseIncident i = quiet();
        i.setConfirmedAnimalCases(25);
        assertTrue(ZoonoticDiseaseAlertRules.alertReason(i).isPresent());
    }

    @Test
    void highSeverityAlerts() {
        ZoonoticDiseaseIncident i = quiet();
        i.setSeverity(Severity.CRITICAL);
        assertTrue(ZoonoticDiseaseAlertRules.alertReason(i).isPresent());
    }

    @Test
    void minorIncidentDoesNotAlert() {
        ZoonoticDiseaseIncident i = quiet();
        i.setClassification(EventClassification.CLUSTER); i.setConfirmedHumanCases(0); i.setConfirmedAnimalCases(2);
        assertFalse(ZoonoticDiseaseAlertRules.alertReason(i).isPresent());
    }
}
