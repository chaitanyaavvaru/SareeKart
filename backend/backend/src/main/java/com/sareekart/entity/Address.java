package com.sareekart.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    private String fullName;
    private String phone;
    private String streetAddress;
    private String city;
    private String state;
    private String pincode;
}
