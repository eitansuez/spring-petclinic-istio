package org.springframework.samples.petclinic.api.boundary.web;

import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.samples.petclinic.api.application.CustomersServiceClient;
import org.springframework.samples.petclinic.api.application.VisitsServiceClient;
import org.springframework.samples.petclinic.api.dto.OwnerDetails;
import org.springframework.samples.petclinic.api.dto.Visits;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
@RequestMapping("/api/gateway")
public class PetClinicController {
    private final CustomersServiceClient customersServiceClient;
    private final VisitsServiceClient visitsServiceClient;

    public PetClinicController(CustomersServiceClient customersServiceClient,
                               VisitsServiceClient visitsServiceClient) {
        this.customersServiceClient = customersServiceClient;
        this.visitsServiceClient = visitsServiceClient;
    }

    @GetMapping(value = "owners/{ownerId}")
    public Mono<OwnerDetails> getOwnerDetails(final @PathVariable int ownerId) {
        return customersServiceClient.getOwner(ownerId)
            .flatMap(owner ->
                visitsServiceClient.getVisitsForPets(owner.getPetIds())
                    .transform(it -> it.onErrorResume(throwable -> {
                        HttpStatusCode responseStatus = ((WebClientResponseException) throwable).getStatusCode();
                        boolean timedOut = (responseStatus == HttpStatus.GATEWAY_TIMEOUT);
                        if (timedOut) {
                            log.info("Response from visits service is a 504, returning an empty visits response as fallback..");
                        }
                        return timedOut;
                    }, throwable -> emptyVisitsForPets()))
                    .map(addVisitsToOwner(owner))
            );
    }

    private Function<Visits, OwnerDetails> addVisitsToOwner(OwnerDetails owner) {
        return visits -> {
            owner.getPets()
                .forEach(pet -> pet.getVisits()
                    .addAll(visits.getItems().stream()
                        .filter(v -> v.getPetId() == pet.getId()).toList())
                );
            return owner;
        };
    }

    private Mono<Visits> emptyVisitsForPets() {
        return Mono.just(new Visits());
    }
}
