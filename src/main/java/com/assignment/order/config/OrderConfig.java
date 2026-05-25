package com.assignment.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

@Configuration
public class OrderConfig {
    @Bean
    public RecordMessageConverter multiTypeConverter() {
        // Automatically maps the JSON payload into your local Order listener model
        // completely ignoring the inbound package structural header token.
        return new StringJsonMessageConverter();
    }
}
