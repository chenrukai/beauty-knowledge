package com.beauty.knowledge.config;

import com.beauty.knowledge.common.constant.RabbitMQConstant;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    @Bean
    public DirectExchange processDlx() {
        return new DirectExchange(RabbitMQConstant.PROCESS_DLX, true, false);
    }

    @Bean
    public Queue processDlq() {
        return new Queue(RabbitMQConstant.PROCESS_DLQ, true);
    }

    @Bean
    public DirectExchange processExchange() {
        return new DirectExchange(RabbitMQConstant.PROCESS_EXCHANGE, true, false);
    }

    @Bean
    public Queue processQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", RabbitMQConstant.PROCESS_DLX);
        args.put("x-dead-letter-routing-key", RabbitMQConstant.DLQ_ROUTING_KEY);
        args.put("x-message-ttl", 300000);
        return new Queue(RabbitMQConstant.PROCESS_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding processBinding(Queue processQueue, DirectExchange processExchange) {
        return BindingBuilder.bind(processQueue)
                .to(processExchange)
                .with(RabbitMQConstant.PROCESS_ROUTING_KEY);
    }

    @Bean
    public Binding processDlqBinding(Queue processDlq, DirectExchange processDlx) {
        return BindingBuilder.bind(processDlq)
                .to(processDlx)
                .with(RabbitMQConstant.DLQ_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(1);
        factory.setMessageConverter(messageConverter);
        return factory;
    }
}
