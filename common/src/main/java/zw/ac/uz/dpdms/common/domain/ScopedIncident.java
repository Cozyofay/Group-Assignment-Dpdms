package zw.ac.uz.dpdms.common.domain;

/** The minimum an incident must expose for the access policy to make decisions about it. */
public interface ScopedIncident {
    String getWard();

    String getReporterUsername();

    IncidentStatus getStatus();
}
