package zw.ac.uz.dpdms.alert.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zw.ac.uz.dpdms.common.messaging.AlertMessaging;

/**
 * Declares the alert queue and its dead-letter queue. Messages that keep failing land in the DLQ
 * instead of blocking the queue, so a broken mail server cannot stop the system.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange alertExchange() {
        return new TopicExchange(AlertMessaging.EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange alertDeadLetterExchange() {
        return new TopicExchange(AlertMessaging.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue alertQueue() {
        return QueueBuilder.durable(AlertMessaging.QUEUE)
                .deadLetterExchange(AlertMessaging.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(AlertMessaging.ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue alertDeadLetterQueue() {
        return QueueBuilder.durable(AlertMessaging.DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding alertBinding() {
        return BindingBuilder.bind(alertQueue()).to(alertExchange()).with(AlertMessaging.ROUTING_KEY);
    }

    @Bean
    public Binding alertDeadLetterBinding() {
        return BindingBuilder.bind(alertDeadLetterQueue()).to(alertDeadLetterExchange())
                .with(AlertMessaging.ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
