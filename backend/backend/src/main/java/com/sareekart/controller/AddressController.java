package com.sareekart.controller;

import com.sareekart.dto.common.ApiResponse;
import com.sareekart.dto.address.AddressRequest;
import com.sareekart.dto.address.AddressResponse;
import com.sareekart.entity.enums.AddressType;
import com.sareekart.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses", description = "Address APIs")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(summary = "Get user's addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(@AuthenticationPrincipal UserDetails userDetails) {
        List<AddressResponse> addresses = addressService.getUserAddresses(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }

    @GetMapping("/default")
    @Operation(summary = "Get default address")
    public ResponseEntity<ApiResponse<AddressResponse>> getDefaultAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "SHIPPING") AddressType type) {
        AddressResponse address = addressService.getDefaultAddress(userDetails.getUsername(), type);
        return ResponseEntity.ok(ApiResponse.success(address));
    }

    @PostMapping
    @Operation(summary = "Add new address")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse address = addressService.addAddress(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Address added successfully", address));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update address")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse address = addressService.updateAddress(userDetails.getUsername(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Address updated successfully", address));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        addressService.deleteAddress(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully", null));
    }

    @PatchMapping("/{id}/default")
    @Operation(summary = "Set address as default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        AddressResponse address = addressService.setDefaultAddress(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Default address updated", address));
    }
}