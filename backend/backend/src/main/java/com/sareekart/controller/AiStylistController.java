package com.sareekart.controller;

import com.sareekart.dto.request.ConsultationQuizRequest;
import com.sareekart.dto.request.DrapeStyleRequest;
import com.sareekart.dto.request.StylistChatRequest;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.DrapeStyleResponse;
import com.sareekart.dto.response.ProductResponse;
import com.sareekart.dto.response.StylistChatResponse;
import com.sareekart.entity.User;
import com.sareekart.service.AiStylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/stylist")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class AiStylistController {

    private final AiStylistService aiStylistService;

    @PostMapping("/drape")
    public ResponseEntity<ApiResponse<DrapeStyleResponse>> styleDrape(
            @Valid @RequestBody DrapeStyleRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Client requested AI drape styling for saree: {}", request.getSareeName());
        DrapeStyleResponse response = aiStylistService.generateDrapeStyling(request, user);
        return ResponseEntity.ok(ApiResponse.success("AI drape styling generated successfully", response));
    }

    @PostMapping("/consult")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> consultQuiz(
            @Valid @RequestBody ConsultationQuizRequest request) {
        log.info("Client submitted AI style consultation quiz for occasion: {}", request.getOccasion());
        List<ProductResponse> matches = aiStylistService.consultStyleQuiz(request);
        return ResponseEntity.ok(ApiResponse.success("Style consultation matches retrieved", matches));
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<StylistChatResponse>> chatWithStylist(
            @Valid @RequestBody StylistChatRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Client initiated AI stylist chat session: {}", request.getSessionId());
        StylistChatResponse response = aiStylistService.chatWithStylist(request, user);
        return ResponseEntity.ok(ApiResponse.success("AI stylist consultation generated successfully", response));
    }

    @PostMapping("/track-tailoring/{consultationId}")
    public ResponseEntity<ApiResponse<Void>> trackTailoringConversion(@PathVariable Long consultationId) {
        log.info("Client converted AI styling consultation {} to tailoring studio", consultationId);
        aiStylistService.recordTailoringConversion(consultationId);
        return ResponseEntity.ok(ApiResponse.success("Tailoring conversion recorded", null));
    }
}
