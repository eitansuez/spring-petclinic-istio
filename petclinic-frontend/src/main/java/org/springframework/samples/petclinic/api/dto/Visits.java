package org.springframework.samples.petclinic.api.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Value;

@Value
public class Visits {
    List<VisitDetails> items = new ArrayList<>();
}
