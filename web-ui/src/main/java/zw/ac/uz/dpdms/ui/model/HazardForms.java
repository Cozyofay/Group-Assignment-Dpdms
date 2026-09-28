package zw.ac.uz.dpdms.ui.model;

import zw.ac.uz.dpdms.common.domain.HazardType;

import java.util.List;
import java.util.Map;

/**
 * The five indicators of each hazard, described once so that a single Thymeleaf form can capture
 * any hazard. The names match the JSON field names of each hazard service's API.
 */
public final class HazardForms {

    private static final Map<HazardType, List<FormField>> FIELDS = Map.of(
            HazardType.FLOOD, List.of(
                    FormField.number("peakWaterLevelMetres", "Peak water level (m)", "0.1", "Highest level reached"),
                    FormField.text("riverBasin", "River basin / catchment", "e.g. Mazowe, Ruya"),
                    FormField.number("householdsDisplaced", "Households displaced", "1", null),
                    FormField.number("areaFloodedHectares", "Area flooded (ha)", "0.1", null),
                    FormField.number("inundationDurationDays", "Duration of inundation (days)", "1", null)),

            HazardType.DROUGHT, List.of(
                    FormField.number("rainfallDeficitMm", "Rainfall deficit (mm)", "0.1", "Against the seasonal norm"),
                    FormField.number("consecutiveDryDays", "Consecutive dry days", "1", null),
                    FormField.number("cropFailurePercentage", "Crop failure (%)", "0.1", "0 to 100"),
                    FormField.number("peopleFacingWaterShortages", "People facing water shortages", "1", null),
                    FormField.number("livestockMortalityCount", "Livestock mortality count", "1", null)),

            HazardType.FIRE, List.of(
                    FormField.number("areaBurnedHectares", "Area burned (ha)", "0.1", null),
                    FormField.select("suspectedCause", "Suspected cause",
                            List.of("NATURAL", "ACCIDENTAL", "DELIBERATE"), null),
                    FormField.number("injuriesOrFatalities", "Injuries or fatalities", "1", null),
                    FormField.number("structuresDestroyed", "Structures destroyed", "1", null),
                    FormField.yesNo("stillActive", "Still active?", "Yes = still burning, No = contained")),

            HazardType.ZOONOTIC_DISEASE, List.of(
                    FormField.text("pathogenName", "Pathogen / disease", "e.g. anthrax, rabies, brucellosis"),
                    FormField.text("animalSpeciesAffected", "Animal species affected", "e.g. cattle, dogs"),
                    FormField.number("confirmedHumanCases", "Confirmed human cases", "1", null),
                    FormField.number("confirmedAnimalCases", "Confirmed animal cases", "1", null),
                    FormField.select("classification", "Classification",
                            List.of("CLUSTER", "OUTBREAK"), null)),

            HazardType.MINING_ACCIDENT, List.of(
                    FormField.text("mineName", "Mine name", null),
                    FormField.select("mineType", "Mine type", List.of("FORMAL", "ARTISANAL"), null),
                    FormField.select("accidentType", "Accident type",
                            List.of("COLLAPSE", "GAS_EXPLOSION", "FLOODING", "FALL_OF_GROUND"), null),
                    FormField.number("trappedOrInjuredMiners", "Trapped or injured miners", "1", null),
                    FormField.number("fatalities", "Fatalities", "1", null),
                    FormField.yesNo("rescueOngoing", "Rescue operations ongoing?", null)));

    private HazardForms() {
    }

    public static List<FormField> fieldsFor(HazardType hazard) {
        return FIELDS.getOrDefault(hazard, List.of());
    }

    /** Accepts "floods" or "FLOOD" and returns the hazard, so URLs can stay readable. */
    public static HazardType fromPath(String value) {
        for (HazardType hazard : HazardType.values()) {
            if (hazard.getApiPath().equalsIgnoreCase(value) || hazard.name().equalsIgnoreCase(value)) {
                return hazard;
            }
        }
        throw new IllegalArgumentException("Unknown hazard: " + value);
    }
}
