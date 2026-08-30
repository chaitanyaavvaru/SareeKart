package com.sareekart.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 100)
    private String slug;

    @Size(max = 1000)
    private String description;

    @Size(max = 500)
    private String imageUrl;

    private Long parentId;

    private Integer sortOrder = 0;

    private Boolean active = true;
}