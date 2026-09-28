package zw.ac.uz.dpdms.common.domain;

/**
 * The five hazard categories monitored by DPDMS. Each one is served by its own micro-service.
 */
public enum HazardType {
    FLOOD("Flood", "floods"),
    DROUGHT("Drought", "droughts"),
    FIRE("Fire", "fires"),
    ZOONOTIC_DISEASE("Zoonotic Disease", "zoonotic-diseases"),
    MINING_ACCIDENT("Mining Accident", "mining-accidents");

    private final String label;
    private final String apiPath;

    HazardType(String label, String apiPath) {
        this.label = label;
        this.apiPath = apiPath;
    }

    /** Human readable name, e.g. "Zoonotic Disease". */
    public String getLabel() {
        return label;
    }

    /** REST resource name, e.g. "zoonotic-diseases" in /api/zoonotic-diseases. */
    public String getApiPath() {
        return apiPath;
    }

    /** Eureka service id, e.g. "zoonotic-disease-service". */
    public String getServiceId() {
        return name().toLowerCase().replace('_', '-') + "-service";
    }
}
