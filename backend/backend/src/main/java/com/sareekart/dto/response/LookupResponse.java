package com.sareekart.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LookupResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String careInstructions;
    private String hexCode;
    private String family;
    private Integer displayOrder;
    private Boolean active;
}
