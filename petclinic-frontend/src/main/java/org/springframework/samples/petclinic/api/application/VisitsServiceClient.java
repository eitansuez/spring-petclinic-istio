package org.springframework.samples.petclinic.api.application;

import static java.util.stream.Collectors.joining;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.samples.petclinic.api.dto.Visits;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class VisitsServiceClient {

  @Value("${visits-service-id://visits-service}")
  private String hostname;

  private final WebClient.Builder webClientBuilder;

  public Mono<Visits> getVisitsForPets(final List<Integer> petIds) {
    return webClientBuilder.build()
        .get()
        .uri(hostname + "/pets/visits?petId={petId}", joinIds(petIds))
        .retrieve()
        .bodyToMono(Visits.class);
  }

  private String joinIds(List<Integer> petIds) {
    return petIds.stream().map(Object::toString).collect(joining(","));
  }

  void setHostname(String hostname) {
    this.hostname = hostname;
  }
}
