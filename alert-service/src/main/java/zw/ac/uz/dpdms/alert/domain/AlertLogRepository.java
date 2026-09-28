package zw.ac.uz.dpdms.alert.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AlertLogRepository extends JpaRepository<AlertLog, Long>, JpaSpecificationExecutor<AlertLog> {

    boolean existsByEventIdAndChannelAndRecipientUsername(String eventId, AlertChannel channel, String recipient);

    long countByStatus(DeliveryStatus status);
}
