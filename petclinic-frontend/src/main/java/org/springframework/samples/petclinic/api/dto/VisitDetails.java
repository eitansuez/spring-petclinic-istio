package org.springframework.samples.petclinic.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VisitDetails {
    private Integer id = null;
    private Integer petId = null;
    private String date = null;
    private String description = null;
}
