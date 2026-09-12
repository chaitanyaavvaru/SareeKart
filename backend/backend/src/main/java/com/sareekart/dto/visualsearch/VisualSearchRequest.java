package com.sareekart.dto.visualsearch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisualSearchRequest {

    private String primaryColor;

    private String secondaryColor;

    private String weaveHint;

    private String occasion;

    @Builder.Default
    private String source = "FILE_UPLOAD";
}
