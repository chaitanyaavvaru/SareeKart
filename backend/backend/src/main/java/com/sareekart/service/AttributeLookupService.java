package com.sareekart.service;

import com.sareekart.dto.response.LookupResponse;
import java.util.List;

public interface AttributeLookupService {
    List<LookupResponse> getAllFabrics(boolean activeOnly);
    List<LookupResponse> getAllOccasions(boolean activeOnly);
    List<LookupResponse> getAllColors(boolean activeOnly);

    LookupResponse getFabricById(Long id);
    LookupResponse getOccasionById(Long id);
    LookupResponse getColorById(Long id);
}
