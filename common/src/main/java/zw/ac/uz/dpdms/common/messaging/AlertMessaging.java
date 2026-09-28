package zw.ac.uz.dpdms.common.messaging;

/** RabbitMQ names shared by the publishers (hazard services) and the consumer (alert-service). */
public final class AlertMessaging {

    public static final String EXCHANGE = "dpdms.alerts";
    public static final String QUEUE = "dpdms.alerts.dispatch";
    public static final String DEAD_LETTER_EXCHANGE = "dpdms.alerts.dlx";
    public static final String DEAD_LETTER_QUEUE = "dpdms.alerts.dispatch.dlq";
    public static final String ROUTING_KEY = "incident.alert";

    private AlertMessaging() {
    }
}
