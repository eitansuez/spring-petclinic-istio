package org.springframework.samples.petclinic.vets.web;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.samples.petclinic.vets.model.Vet;
import org.springframework.samples.petclinic.vets.model.VetRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/vets")
@RestController
@RequiredArgsConstructor
class VetResource {
    private final VetRepository vetRepository;

    @GetMapping
    public List<Vet> showResourcesVetList() {
        return vetRepository.findAll();
    }
}
