package com.sareekart.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartMergeRequest {

    @NotNull(message = "Items list cannot be null")
    @Valid
    private List<CartItemRequest> items;
}
