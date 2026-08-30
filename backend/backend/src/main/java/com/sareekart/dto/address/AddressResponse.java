package com.sareekart.dto.address;

import com.sareekart.entity.Address;
import com.sareekart.entity.enums.AddressType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {
    private Long id;
    private AddressType type;
    private String name;
    private String phone;
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String pincode;
    private String country;
    private Boolean isDefault;

    public static AddressResponse from(Address address) {
        if (address == null) return null;
        return AddressResponse.builder()
            .id(address.getId())
            .type(address.getType())
            .name(address.getName())
            .phone(address.getPhone())
            .line1(address.getLine1())
            .line2(address.getLine2())
            .city(address.getCity())
            .state(address.getState())
            .pincode(address.getPincode())
            .country(address.getCountry())
            .isDefault(address.getIsDefault())
            .build();
    }

    public static List<AddressResponse> fromList(List<Address> addresses) {
        return addresses.stream()
            .map(AddressResponse::from)
            .collect(Collectors.toList());
    }
}