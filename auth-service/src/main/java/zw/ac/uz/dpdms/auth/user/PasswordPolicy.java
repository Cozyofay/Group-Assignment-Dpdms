package zw.ac.uz.dpdms.auth.user;

public final class PasswordPolicy {

    /** 8-72 characters, no spaces, at least one letter and one digit. (72 is BCrypt's limit.) */
    public static final String REGEX = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,72}$";
    public static final String MESSAGE = "must be 8-72 characters with at least one letter and one digit, and no spaces";

    private PasswordPolicy() {
    }
}
