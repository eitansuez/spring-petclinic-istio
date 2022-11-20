package org.springframework.samples.petclinic.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Data
public class OwnerDetails {
    private int id;
    private String firstName;
    private String lastName;
    private String address;
    private String city;
    private String telephone;

    private final List<PetDetails> pets = new ArrayList<>();

    @JsonIgnore
    public List<Integer> getPetIds() {
        return pets.stream()
            .map(PetDetails::getId)
            .collect(toList());
    }
}
