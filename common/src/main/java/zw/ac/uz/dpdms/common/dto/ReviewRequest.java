package zw.ac.uz.dpdms.common.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body for approve / reject / request-corrections. A comment is mandatory for reject and corrections. */
public record ReviewRequest(
        @Size(max = 1000)
        @Pattern(regexp = ValidationPatterns.SAFE_LONG_TEXT, message = ValidationPatterns.SAFE_LONG_TEXT_MESSAGE)
        String comment) {
}
