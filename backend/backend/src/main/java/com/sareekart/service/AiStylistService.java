package com.sareekart.service;

import com.sareekart.dto.request.ConsultationQuizRequest;
import com.sareekart.dto.request.DrapeStyleRequest;
import com.sareekart.dto.response.AiStylistTelemetryResponse;
import com.sareekart.dto.response.DrapeStyleResponse;
import com.sareekart.dto.response.ProductResponse;
import com.sareekart.entity.User;

import java.util.List;

public interface AiStylistService {

    DrapeStyleResponse generateDrapeStyling(DrapeStyleRequest request, User user);

    List<ProductResponse> consultStyleQuiz(ConsultationQuizRequest request);

    void recordTailoringConversion(Long consultationId);

    AiStylistTelemetryResponse getTelemetry();
}
