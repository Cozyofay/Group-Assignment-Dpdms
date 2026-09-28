package zw.ac.uz.dpdms.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.ui.model.FormField;
import zw.ac.uz.dpdms.ui.model.HazardForms;
import zw.ac.uz.dpdms.ui.model.SessionUser;
import zw.ac.uz.dpdms.common.domain.Role;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HazardFormsTest {

    @Test
    @DisplayName("Every hazard has its five indicators on the capture form")
    void everyHazardHasItsIndicators() {
        for (HazardType hazard : HazardType.values()) {
            List<FormField> fields = HazardForms.fieldsFor(hazard);
            assertTrue(fields.size() >= 5, hazard + " must have at least five indicator fields");
            fields.forEach(field -> assertFalse(field.name().isBlank()));
        }
    }

    @Test
    void hazardIsResolvedFromTheUrlPath() {
        assertEquals(HazardType.FLOOD, HazardForms.fromPath("floods"));
        assertEquals(HazardType.ZOONOTIC_DISEASE, HazardForms.fromPath("zoonotic-diseases"));
        assertEquals(HazardType.MINING_ACCIDENT, HazardForms.fromPath("MINING_ACCIDENT"));
        assertThrows(IllegalArgumentException.class, () -> HazardForms.fromPath("earthquakes"));
    }

    @Test
    @DisplayName("National users are shown as read-only in the UI")
    void nationalUserIsReadOnly() {
        SessionUser national = new SessionUser("token", "national", "National Officer",
                Role.NATIONAL_VIEWER, null, null);
        assertFalse(national.canWrite());
        assertTrue(national.isNational());

        SessionUser recorder = new SessionUser("token", "recorder.flood.ward1", "Recorder",
                Role.WARD_RECORDER, HazardType.FLOOD, "Ward 1");
        assertTrue(recorder.canWrite());
        assertEquals("Ward recorder - Flood, Ward 1", recorder.roleLabel());
    }
}
