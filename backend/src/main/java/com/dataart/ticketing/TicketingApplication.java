package com.dataart.ticketing;

import java.util.TimeZone;

import com.dataart.ticketing.config.AppProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class TicketingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketingApplication.class, args);
    }

    /** All server-side timestamps are handled in UTC. */
    @PostConstruct
    void forceUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}
