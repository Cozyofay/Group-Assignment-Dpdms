package zw.ac.uz.dpdms.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/** Produces the "what changed" text for the audit trail, e.g. "peakWaterLevel: 2.1 -> 2.4". */
public final class ChangeDiff {

    private static final Set<String> IGNORED = Set.of("id", "createdAt", "updatedAt", "version");

    private ChangeDiff() {
    }

    public static Map<String, Object> snapshot(ObjectMapper mapper, Object entity) {
        return mapper.convertValue(entity, new TypeReference<LinkedHashMap<String, Object>>() {
        });
    }

    public static String describe(Map<String, Object> before, Map<String, Object> after) {
        Set<String> keys = new LinkedHashSet<>(before.keySet());
        keys.addAll(after.keySet());
        StringJoiner joiner = new StringJoiner("; ");
        for (String key : keys) {
            if (IGNORED.contains(key)) {
                continue;
            }
            Object oldValue = before.get(key);
            Object newValue = after.get(key);
            if (!Objects.equals(oldValue, newValue)) {
                joiner.add(key + ": " + oldValue + " -> " + newValue);
            }
        }
        return joiner.length() == 0 ? "No field changes" : joiner.toString();
    }
}
