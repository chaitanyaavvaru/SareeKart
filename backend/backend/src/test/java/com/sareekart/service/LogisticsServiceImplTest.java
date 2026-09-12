package com.sareekart.service;

import com.sareekart.dto.request.PincodeOverrideRequest;
import com.sareekart.dto.response.PincodeLookupResponse;
import com.sareekart.entity.PincodeOverride;
import com.sareekart.enums.LogisticsZone;
import com.sareekart.exception.BadRequestException;
import com.sareekart.repository.PincodeOverrideRepository;
import com.sareekart.service.impl.LogisticsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LogisticsServiceImplTest {

    @Mock
    private PincodeOverrideRepository overrideRepository;

    @InjectMocks
    private LogisticsServiceImpl logisticsService;

    @Test
    @DisplayName("Pincode 560001 (Bengaluru) resolves to Metro Capital Circle via Blue Dart Apex Air")
    void testResolveBengaluruPincode_560001_MetroExpress() {
        when(overrideRepository.findByPincode("560001")).thenReturn(Optional.empty());

        PincodeLookupResponse res = logisticsService.checkPincode("560001");

        assertNotNull(res);
        assertEquals("560001", res.getPincode());
        assertEquals("Bengaluru", res.getCity());
        assertEquals("Karnataka", res.getState());
        assertEquals("METRO", res.getZone());
        assertTrue(res.isServiceable());
        assertTrue(res.isCodAvailable());
        assertEquals("Blue Dart Apex Air", res.getCourierPartner());
        assertEquals(1, res.getTransitDays());
        assertFalse(res.isCustomOverrideApplied());
        assertTrue(res.getFulfillmentHub().contains("Bengaluru"));
    }

    @Test
    @DisplayName("Pincode 631501 (Kanchipuram) resolves to Tier-1 Hub with COD available")
    void testResolveKanchipuramPincode_631501_SouthGuildAtelier() {
        when(overrideRepository.findByPincode("631501")).thenReturn(Optional.empty());

        PincodeLookupResponse res = logisticsService.checkPincode("631501");

        assertNotNull(res);
        assertEquals("631501", res.getPincode());
        assertEquals("Kanchipuram / Salem", res.getCity());
        assertEquals("Tamil Nadu", res.getState());
        assertTrue(res.isServiceable());
        assertTrue(res.isCodAvailable());
        assertEquals("Blue Dart Apex", res.getCourierPartner());
    }

    @Test
    @DisplayName("Pincode 110001 (New Delhi) resolves to Delhi Aviation Gateway")
    void testResolveDelhiPincode_110001_NorthGateway() {
        when(overrideRepository.findByPincode("110001")).thenReturn(Optional.empty());

        PincodeLookupResponse res = logisticsService.checkPincode("110001");

        assertNotNull(res);
        assertEquals("110001", res.getPincode());
        assertEquals("New Delhi", res.getCity());
        assertEquals("Delhi", res.getState());
        assertEquals("Blue Dart Apex Air", res.getCourierPartner());
        assertTrue(res.isServiceable());
        assertTrue(res.isCodAvailable());
    }

    @Test
    @DisplayName("Pincode 221001 (Varanasi) resolves to Varanasi Guild Fulfillment Center via Delhivery")
    void testResolveVaranasiPincode_221001_EastCentral() {
        when(overrideRepository.findByPincode("221001")).thenReturn(Optional.empty());

        PincodeLookupResponse res = logisticsService.checkPincode("221001");

        assertNotNull(res);
        assertEquals("221001", res.getPincode());
        assertEquals("Varanasi", res.getCity());
        assertEquals("Uttar Pradesh", res.getState());
        assertEquals("Delhivery Express", res.getCourierPartner());
        assertTrue(res.isServiceable());
    }

    @Test
    @DisplayName("Pincode 194101 (Leh Ladakh) resolves to Remote Zone with COD disabled")
    void testResolveLehLadakhPincode_194101_RemoteNoCod() {
        when(overrideRepository.findByPincode("194101")).thenReturn(Optional.empty());

        PincodeLookupResponse res = logisticsService.checkPincode("194101");

        assertNotNull(res);
        assertEquals("194101", res.getPincode());
        assertEquals("Srinagar / Ladakh", res.getCity());
        assertEquals("Jammu & Kashmir", res.getState());
        assertEquals("REMOTE", res.getZone());
        assertTrue(res.isServiceable());
        assertFalse(res.isCodAvailable(), "Remote corridors must have COD disabled");
        assertEquals("DTDC Air Express", res.getCourierPartner());
        assertEquals(5, res.getTransitDays());
    }

    @Test
    @DisplayName("Invalid pincode formats throw BadRequestException")
    void testResolveInvalidPincode_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> logisticsService.checkPincode(null));
        assertThrows(BadRequestException.class, () -> logisticsService.checkPincode(""));
        assertThrows(BadRequestException.class, () -> logisticsService.checkPincode("12345"));
        assertThrows(BadRequestException.class, () -> logisticsService.checkPincode("ABCDEF"));
        assertThrows(BadRequestException.class, () -> logisticsService.checkPincode("5600019"));
    }

    @Test
    @DisplayName("Custom DB Override overrides default logistics matrix settings")
    void testCustomPincodeOverride_AppliesStaffSettings() {
        PincodeOverride override = PincodeOverride.builder()
                .id(1L)
                .pincode("560001")
                .city("Bengaluru Central")
                .state("Karnataka")
                .zone(LogisticsZone.METRO)
                .serviceable(true)
                .codAvailable(false)
                .courierPartner("SareeKart White-Glove VIP Courier")
                .transitDays(1)
                .notes("VIP corridor express")
                .build();

        when(overrideRepository.findByPincode("560001")).thenReturn(Optional.of(override));

        PincodeLookupResponse res = logisticsService.checkPincode("560001");

        assertNotNull(res);
        assertTrue(res.isCustomOverrideApplied());
        assertFalse(res.isCodAvailable());
        assertEquals("SareeKart White-Glove VIP Courier", res.getCourierPartner());
        assertEquals(1, res.getTransitDays());
    }

    @Test
    @DisplayName("Save override persists successfully via repository")
    void testSaveOverride_ValidRequest_Success() {
        PincodeOverrideRequest req = PincodeOverrideRequest.builder()
                .pincode("600001")
                .city("Chennai")
                .state("Tamil Nadu")
                .zone("METRO")
                .serviceable(true)
                .codAvailable(true)
                .courierPartner("Blue Dart Apex Express")
                .transitDays(2)
                .notes("Chennai Central Hub")
                .build();

        PincodeOverride savedEntity = PincodeOverride.builder()
                .id(2L)
                .pincode("600001")
                .city("Chennai")
                .state("Tamil Nadu")
                .zone(LogisticsZone.METRO)
                .serviceable(true)
                .codAvailable(true)
                .courierPartner("Blue Dart Apex Express")
                .transitDays(2)
                .build();

        when(overrideRepository.findByPincode("600001")).thenReturn(Optional.empty());
        when(overrideRepository.save(any(PincodeOverride.class))).thenReturn(savedEntity);

        PincodeOverride result = logisticsService.saveOverride(req);

        assertNotNull(result);
        assertEquals("600001", result.getPincode());
        verify(overrideRepository, times(1)).save(any(PincodeOverride.class));
    }

    @Test
    @DisplayName("Delete override invokes repository deletion")
    void testDeleteOverride_CallsRepositoryDelete() {
        PincodeOverride override = PincodeOverride.builder()
                .id(3L)
                .pincode("500001")
                .build();

        when(overrideRepository.findByPincode("500001")).thenReturn(Optional.of(override));
        doNothing().when(overrideRepository).delete(override);

        logisticsService.deleteOverride("500001");

        verify(overrideRepository, times(1)).delete(override);
    }

    @Test
    @DisplayName("Estimated delivery date label is generated and not empty")
    void testEstimatedDeliveryDate_Populated() {
        when(overrideRepository.findByPincode("560001")).thenReturn(Optional.empty());

        PincodeLookupResponse res = logisticsService.checkPincode("560001");

        assertNotNull(res.getEstimatedDeliveryDate());
        assertFalse(res.getEstimatedDeliveryDate().isBlank());
        assertNotNull(res.getDeliveryWindowLabel());
    }
}
