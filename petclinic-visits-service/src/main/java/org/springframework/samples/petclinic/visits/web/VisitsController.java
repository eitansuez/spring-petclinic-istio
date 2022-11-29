package org.springframework.samples.petclinic.visits.web;

import java.util.List;
import jakarta.validation.Valid;

import io.micrometer.core.annotation.Timed;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.visits.model.Visit;
import org.springframework.samples.petclinic.visits.model.VisitRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@Timed("petclinic.visit")
class VisitsController {
    private final VisitRepository visitRepository;

    private final int delayMillis;

    VisitsController(VisitRepository visitRepository,
                     @org.springframework.beans.factory.annotation.Value("${delay.millis:0}") int delayMillis) {
        this.visitRepository = visitRepository;
        this.delayMillis = delayMillis;
    }

    @PostMapping("owners/*/pets/{petId}/visits")
    @ResponseStatus(HttpStatus.CREATED)
    Visit create(
        @Valid @RequestBody Visit visit,
        @PathVariable("petId") int petId) {

        visit.setPetId(petId);
        log.info("Saving visit {}", visit);
        return visitRepository.save(visit);
    }

    @GetMapping("owners/*/pets/{petId}/visits")
    List<Visit> visits(@PathVariable("petId") int petId) {
        return visitRepository.findByPetId(petId);
    }

    @GetMapping("pets/visits")
    Visits visitsMultiGet(@RequestParam("petId") List<Integer> petIds) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException ignored) { }
        final List<Visit> byPetIdIn = visitRepository.findByPetIdIn(petIds);
        return new Visits(byPetIdIn);
    }

    @Value
    static class Visits {
        List<Visit> items;
    }
}
