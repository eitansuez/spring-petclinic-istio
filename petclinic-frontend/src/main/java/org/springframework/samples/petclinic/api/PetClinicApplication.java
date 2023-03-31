package org.springframework.samples.petclinic.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class PetClinicApplication {
  public static void main(String[] args) {
    SpringApplication.run(PetClinicApplication.class, args);
  }

  @Bean
  public WebClient webClient(WebClient.Builder builder) {
    return builder.build();
  }
}
