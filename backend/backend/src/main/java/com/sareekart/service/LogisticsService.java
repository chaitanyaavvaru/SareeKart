package com.sareekart.service;

import com.sareekart.dto.request.PincodeOverrideRequest;
import com.sareekart.dto.response.PincodeLookupResponse;
import com.sareekart.entity.PincodeOverride;

import java.util.List;

public interface LogisticsService {
    PincodeLookupResponse checkPincode(String pincode);
    boolean isServiceable(String pincode);
    boolean isCodAvailable(String pincode);

    // Staff admin operations
    List<PincodeOverride> getAllOverrides();
    PincodeOverride saveOverride(PincodeOverrideRequest request);
    void deleteOverride(String pincode);
}
