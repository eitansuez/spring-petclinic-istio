package org.springframework.samples.petclinic.visits;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
public class VisitsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(VisitsServiceApplication.class, args);
    }
}
