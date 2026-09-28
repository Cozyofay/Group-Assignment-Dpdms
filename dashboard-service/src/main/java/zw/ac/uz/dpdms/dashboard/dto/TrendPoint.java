package zw.ac.uz.dpdms.dashboard.dto;

import java.util.Map;

/** One month of the trend chart: the period label, the total, and the split by hazard. */
public record TrendPoint(String period, long total, Map<String, Long> byHazard) {
}
