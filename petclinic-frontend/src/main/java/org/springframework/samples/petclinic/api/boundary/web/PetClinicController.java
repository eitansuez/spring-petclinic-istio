package org.springframework.samples.petclinic.api.boundary.web;

import java.util.function.Function;

import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.samples.petclinic.api.application.CustomersServiceClient;
import org.springframework.samples.petclinic.api.application.VisitsServiceClient;
import org.springframework.samples.petclinic.api.dto.OwnerDetails;
import org.springframework.samples.petclinic.api.dto.Visits;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/gateway")
public class PetClinicController {

    private final CustomersServiceClient customersServiceClient;
    private final VisitsServiceClient visitsServiceClient;
    private final ReactiveCircuitBreakerFactory<?, ?> cbFactory;

    public PetClinicController(CustomersServiceClient customersServiceClient,
                               VisitsServiceClient visitsServiceClient,
                               ReactiveCircuitBreakerFactory<?, ?> cbFactory) {
        this.customersServiceClient = customersServiceClient;
        this.visitsServiceClient = visitsServiceClient;
        this.cbFactory = cbFactory;
    }

    @GetMapping(value = "owners/{ownerId}")
    public Mono<OwnerDetails> getOwnerDetails(final @PathVariable int ownerId) {
        return customersServiceClient.getOwner(ownerId)
            .flatMap(owner ->
                visitsServiceClient.getVisitsForPets(owner.getPetIds())
                    .transform(it -> {
                        ReactiveCircuitBreaker cb = cbFactory.create("getOwnerDetails");
                        return cb.run(it, throwable -> emptyVisitsForPets());
                    })
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
