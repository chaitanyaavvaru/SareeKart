package com.sareekart.dto.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sareekart.entity.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Immutable snapshot of a shipping/billing address persisted as JSON on the
 * order row (shipping_address_json / billing_address_json columns).
 *
 * Field names deliberately match the frontend contract
 * (fullName, streetAddress) so admin/customer pages can render snapshots
 * without remapping.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class AddressSnapshot {

    private String fullName;
    private String phone;
    private String streetAddress;
    private String landmark;
    private String city;
    private String state;
    private String pincode;
    private String country;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static AddressSnapshot from(Address address) {
        if (address == null) return null;
        return AddressSnapshot.builder()
            .fullName(address.getName())
            .phone(address.getPhone())
            .streetAddress(address.getLine1())
            .landmark(address.getLine2())
            .city(address.getCity())
            .state(address.getState())
            .pincode(address.getPincode())
            .country(address.getCountry())
            .build();
    }

    public static AddressSnapshot fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, AddressSnapshot.class);
        } catch (Exception e) {
            log.warn("Failed to parse address snapshot JSON: {}", e.getMessage());
            return null;
        }
    }

    public static String toJson(AddressSnapshot snapshot) {
        try {
            return MAPPER.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize address snapshot", e);
        }
    }
}