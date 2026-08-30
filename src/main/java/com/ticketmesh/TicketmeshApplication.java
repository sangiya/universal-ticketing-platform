package com.ticketmesh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TicketmeshApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketmeshApplication.class, args);
    }
}
