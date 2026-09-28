package zw.ac.uz.dpdms.auth.internal;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import zw.ac.uz.dpdms.auth.user.UserService;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.dto.AlertRecipient;

import java.util.List;

/**
 * Service-to-service API (requires X-Internal-Api-Key, never routed by the gateway).
 * The alert-service calls this to learn who must be notified about an incident.
 */
@Hidden
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/alert-recipients")
    public List<AlertRecipient> alertRecipients(@RequestParam HazardType hazard, @RequestParam String ward) {
        return userService.alertRecipients(hazard, ward);
    }
}
