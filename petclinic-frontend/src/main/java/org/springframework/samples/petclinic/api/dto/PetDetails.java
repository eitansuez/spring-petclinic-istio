package org.springframework.samples.petclinic.api.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PetDetails {
    private int id;
    private String name;
    private String birthDate;
    private PetType type;
    private final List<VisitDetails> visits = new ArrayList<>();
}
