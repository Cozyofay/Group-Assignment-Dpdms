package zw.ac.uz.dpdms.ui.model;

import java.util.List;

/** One input on the capture form. */
public record FormField(String name, String label, String type, String step, String help, List<String> options) {

    public static FormField number(String name, String label, String step, String help) {
        return new FormField(name, label, "number", step, help, List.of());
    }

    public static FormField text(String name, String label, String help) {
        return new FormField(name, label, "text", null, help, List.of());
    }

    public static FormField select(String name, String label, List<String> options, String help) {
        return new FormField(name, label, "select", null, help, options);
    }

    public static FormField yesNo(String name, String label, String help) {
        return new FormField(name, label, "select", null, help, List.of("true", "false"));
    }
}
