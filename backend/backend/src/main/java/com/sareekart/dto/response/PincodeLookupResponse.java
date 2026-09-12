package com.sareekart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PincodeLookupResponse {
    private String pincode;
    private String city;
    private String state;
    private String zone;
    private String zoneDisplayName;
    private boolean serviceable;
    private boolean codAvailable;
    private String courierPartner;
    private Integer transitDays;
    private String estimatedDeliveryDate;
    private String deliveryWindowLabel;
    private String fulfillmentHub;
    private boolean customOverrideApplied;
}
