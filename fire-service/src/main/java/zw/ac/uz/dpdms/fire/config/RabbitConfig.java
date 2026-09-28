package zw.ac.uz.dpdms.fire.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zw.ac.uz.dpdms.common.messaging.AlertMessaging;

@Configuration
public class RabbitConfig {

    /** Durable so queued alerts survive a broker restart. */
    @Bean
    public TopicExchange alertExchange() {
        return new TopicExchange(AlertMessaging.EXCHANGE, true, false);
    }

    /** Send alerts as JSON rather than Java serialization, so any service can read them. */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
