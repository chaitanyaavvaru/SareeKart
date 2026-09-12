package com.sareekart.service.impl;

import com.sareekart.dto.request.PincodeOverrideRequest;
import com.sareekart.dto.response.PincodeLookupResponse;
import com.sareekart.entity.PincodeOverride;
import com.sareekart.enums.LogisticsZone;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.repository.PincodeOverrideRepository;
import com.sareekart.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogisticsServiceImpl implements LogisticsService {

    private final PincodeOverrideRepository pincodeOverrideRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.ENGLISH);

    @Override
    @Transactional(readOnly = true)
    public PincodeLookupResponse checkPincode(String pincode) {
        if (pincode == null || pincode.trim().isEmpty()) {
            throw new BadRequestException("PIN code is mandatory");
        }

        String cleanPin = pincode.trim();
        if (!cleanPin.matches("^[0-9]{6}$")) {
            throw new BadRequestException("Invalid PIN code format. Must be exactly 6 numeric digits.");
        }

        // 1. Check database overrides first
        Optional<PincodeOverride> overrideOpt = pincodeOverrideRepository.findByPincode(cleanPin);
        if (overrideOpt.isPresent()) {
            PincodeOverride po = overrideOpt.get();
            LocalDate eta = computeDeliveryDate(po.getTransitDays());
            return PincodeLookupResponse.builder()
                    .pincode(cleanPin)
                    .city(po.getCity())
                    .state(po.getState())
                    .zone(po.getZone().name())
                    .zoneDisplayName(po.getZone().getDisplayName())
                    .serviceable(Boolean.TRUE.equals(po.getServiceable()))
                    .codAvailable(Boolean.TRUE.equals(po.getCodAvailable()))
                    .courierPartner(po.getCourierPartner())
                    .transitDays(po.getTransitDays())
                    .estimatedDeliveryDate(formatEtaDate(eta))
                    .deliveryWindowLabel(po.getTransitDays() <= 2 ? "24-48 Hours Express Delivery" : po.getTransitDays() + " Business Days Standard Delivery")
                    .fulfillmentHub("Central Atelier Logistics Network")
                    .customOverrideApplied(true)
                    .build();
        }

        // 2. Dynamic resolution using Indian Postal Circle & Prefix mapping
        int prefix = Integer.parseInt(cleanPin.substring(0, 2));
        String city;
        String state;
        LogisticsZone zone;
        String courier;
        int transitDays;
        boolean cod = true;
        String hub = "Bengaluru Central Hub (WH-01)";

        if (prefix == 11) {
            city = "New Delhi";
            state = "Delhi";
            zone = LogisticsZone.METRO;
            courier = "Blue Dart Apex Air";
            transitDays = 2;
            hub = "Delhi NCR Aviation Gateway";
        } else if (prefix == 12 || prefix == 13) {
            city = prefix == 12 ? "Gurugram / Faridabad" : "Panipat / Karnal";
            state = "Haryana";
            zone = LogisticsZone.TIER_1;
            courier = "Delhivery Express";
            transitDays = 2;
        } else if (prefix >= 14 && prefix <= 16) {
            city = prefix == 16 ? "Chandigarh" : "Amritsar / Ludhiana";
            state = "Punjab";
            zone = LogisticsZone.TIER_1;
            courier = "Delhivery Express";
            transitDays = 2;
        } else if (prefix == 17) {
            city = "Shimla / Dharamshala";
            state = "Himachal Pradesh";
            zone = LogisticsZone.TIER_2;
            courier = "DTDC Air Express";
            transitDays = 3;
        } else if (prefix == 18 || prefix == 19) {
            city = prefix == 18 ? "Jammu" : "Srinagar / Ladakh";
            state = "Jammu & Kashmir";
            zone = LogisticsZone.REMOTE;
            courier = "DTDC Air Express";
            transitDays = 5;
            if (cleanPin.startsWith("194")) {
                cod = false; // High mountain remote Ladakh corridor
            }
        } else if (prefix >= 20 && prefix <= 28) {
            city = (prefix == 22 ? "Varanasi" : (prefix == 20 ? "Noida / Ghaziabad" : "Lucknow / Kanpur"));
            state = (prefix == 24 || prefix == 26) ? "Uttarakhand" : "Uttar Pradesh";
            zone = (prefix == 20 || prefix == 22) ? LogisticsZone.TIER_1 : LogisticsZone.TIER_2;
            courier = "Delhivery Express";
            transitDays = 2;
            hub = "Varanasi Guild Fulfillment Center (WH-02)";
        } else if (prefix >= 30 && prefix <= 34) {
            city = prefix == 30 ? "Jaipur" : (prefix == 34 ? "Jodhpur" : "Udaipur / Ajmer");
            state = "Rajasthan";
            zone = LogisticsZone.TIER_1;
            courier = "Delhivery Express";
            transitDays = 3;
        } else if (prefix >= 36 && prefix <= 39) {
            city = prefix == 38 ? "Ahmedabad" : (prefix == 39 ? "Surat" : "Vadodara / Rajkot");
            state = "Gujarat";
            zone = LogisticsZone.TIER_1;
            courier = "Blue Dart Apex Air";
            transitDays = 2;
        } else if (prefix == 40) {
            city = "Mumbai";
            state = "Maharashtra";
            zone = LogisticsZone.METRO;
            courier = "Blue Dart Apex Air";
            transitDays = 1;
            hub = "Mumbai West Coastal Hub";
        } else if (prefix == 41) {
            city = "Pune";
            state = "Maharashtra";
            zone = LogisticsZone.TIER_1;
            courier = "Blue Dart Apex";
            transitDays = 2;
        } else if (prefix >= 42 && prefix <= 44) {
            city = prefix == 44 ? "Nagpur" : "Nashik / Aurangabad";
            state = "Maharashtra";
            zone = LogisticsZone.TIER_2;
            courier = "Delhivery Express";
            transitDays = 3;
        } else if (prefix >= 45 && prefix <= 49) {
            city = prefix == 45 ? "Indore" : (prefix == 46 ? "Bhopal" : "Raipur / Bilaspur");
            state = prefix <= 48 ? "Madhya Pradesh" : "Chhattisgarh";
            zone = LogisticsZone.TIER_2;
            courier = "Delhivery Express";
            transitDays = 3;
        } else if (prefix == 50) {
            city = "Hyderabad";
            state = "Telangana";
            zone = LogisticsZone.METRO;
            courier = "Blue Dart Apex";
            transitDays = 1;
            hub = "Hyderabad Deccan Gateway";
        } else if (prefix >= 51 && prefix <= 53) {
            city = prefix == 53 ? "Visakhapatnam" : (prefix == 52 ? "Vijayawada" : "Tirupati");
            state = "Andhra Pradesh";
            zone = LogisticsZone.TIER_1;
            courier = "Blue Dart Apex";
            transitDays = 2;
        } else if (prefix == 56) {
            city = "Bengaluru";
            state = "Karnataka";
            zone = LogisticsZone.METRO;
            courier = "Blue Dart Apex Air";
            transitDays = 1;
            hub = "Bengaluru Central Fulfillment Hub (WH-01)";
        } else if (prefix >= 57 && prefix <= 59) {
            city = prefix == 57 ? "Mysuru / Mangaluru" : "Hubballi / Belagavi";
            state = "Karnataka";
            zone = LogisticsZone.TIER_1;
            courier = "Blue Dart Apex";
            transitDays = 2;
        } else if (prefix == 60) {
            city = "Chennai";
            state = "Tamil Nadu";
            zone = LogisticsZone.METRO;
            courier = "Blue Dart Apex Air";
            transitDays = 1;
            hub = "Kanchipuram Reserve Atelier (WH-03)";
        } else if (prefix >= 61 && prefix <= 66) {
            city = prefix == 64 ? "Coimbatore" : (prefix == 62 ? "Madurai" : (prefix == 63 ? "Kanchipuram / Salem" : "Tiruchirappalli"));
            state = "Tamil Nadu";
            zone = LogisticsZone.TIER_1;
            courier = "Blue Dart Apex";
            transitDays = 1;
            hub = "Kanchipuram Reserve Atelier (WH-03)";
        } else if (prefix >= 67 && prefix <= 69) {
            city = prefix == 68 ? "Kochi / Ernakulam" : (prefix == 69 ? "Thiruvananthapuram" : "Kozhikode");
            state = "Kerala";
            zone = LogisticsZone.TIER_1;
            courier = "Blue Dart Apex";
            transitDays = 2;
        } else if (prefix == 70) {
            city = "Kolkata";
            state = "West Bengal";
            zone = LogisticsZone.METRO;
            courier = "Blue Dart Air Cargo";
            transitDays = 2;
            hub = "Kolkata Eastern Gateway";
        } else if (prefix >= 71 && prefix <= 74) {
            city = "Howrah / Siliguri";
            state = "West Bengal";
            zone = LogisticsZone.TIER_2;
            courier = "Delhivery Express";
            transitDays = 3;
        } else if (prefix >= 75 && prefix <= 77) {
            city = "Bhubaneswar / Cuttack";
            state = "Odisha";
            zone = LogisticsZone.TIER_2;
            courier = "Delhivery Express";
            transitDays = 3;
        } else if (prefix >= 78 && prefix <= 79) {
            city = prefix == 78 ? "Guwahati" : "Shillong / Agartala";
            state = prefix == 78 ? "Assam" : "North-Eastern States";
            zone = LogisticsZone.REGIONAL;
            courier = "DTDC Air Express";
            transitDays = 4;
        } else if (prefix >= 80 && prefix <= 85) {
            city = prefix == 80 ? "Patna" : (prefix == 83 ? "Ranchi / Jamshedpur" : "Gaya / Muzaffarpur");
            state = prefix <= 82 ? "Bihar" : "Jharkhand";
            zone = LogisticsZone.TIER_2;
            courier = "Delhivery Express";
            transitDays = 3;
        } else {
            city = "Domestic Destination";
            state = "India";
            zone = LogisticsZone.REGIONAL;
            courier = "Delhivery Express";
            transitDays = 4;
        }

        LocalDate eta = computeDeliveryDate(transitDays);
        String windowLabel = transitDays == 1 
                ? "Next Day Priority Delivery" 
                : (transitDays == 2 ? "24-48 Hours Express Delivery" : transitDays + " Business Days Standard Delivery");

        return PincodeLookupResponse.builder()
                .pincode(cleanPin)
                .city(city)
                .state(state)
                .zone(zone.name())
                .zoneDisplayName(zone.getDisplayName())
                .serviceable(true)
                .codAvailable(cod)
                .courierPartner(courier)
                .transitDays(transitDays)
                .estimatedDeliveryDate(formatEtaDate(eta))
                .deliveryWindowLabel(windowLabel)
                .fulfillmentHub(hub)
                .customOverrideApplied(false)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isServiceable(String pincode) {
        try {
            PincodeLookupResponse resp = checkPincode(pincode);
            return resp.isServiceable();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCodAvailable(String pincode) {
        try {
            PincodeLookupResponse resp = checkPincode(pincode);
            return resp.isServiceable() && resp.isCodAvailable();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PincodeOverride> getAllOverrides() {
        return pincodeOverrideRepository.findAllByOrderByPincodeAsc();
    }

    @Override
    @Transactional
    public PincodeOverride saveOverride(PincodeOverrideRequest request) {
        String pin = request.getPincode().trim();
        PincodeOverride override = pincodeOverrideRepository.findByPincode(pin)
                .orElseGet(() -> PincodeOverride.builder().pincode(pin).build());

        override.setCity(request.getCity().trim());
        override.setState(request.getState().trim());
        if (request.getZone() != null && !request.getZone().isBlank()) {
            try {
                override.setZone(LogisticsZone.valueOf(request.getZone().trim().toUpperCase()));
            } catch (Exception ignored) {}
        }
        if (request.getServiceable() != null) override.setServiceable(request.getServiceable());
        if (request.getCodAvailable() != null) override.setCodAvailable(request.getCodAvailable());
        if (request.getCourierPartner() != null && !request.getCourierPartner().isBlank()) {
            override.setCourierPartner(request.getCourierPartner().trim());
        }
        if (request.getTransitDays() != null && request.getTransitDays() > 0) {
            override.setTransitDays(request.getTransitDays());
        }
        if (request.getNotes() != null) override.setNotes(request.getNotes().trim());

        PincodeOverride saved = pincodeOverrideRepository.save(override);
        log.info("Saved logistics override for PIN {}", pin);
        return saved;
    }

    @Override
    @Transactional
    public void deleteOverride(String pincode) {
        pincodeOverrideRepository.findByPincode(pincode.trim())
                .ifPresent(pincodeOverrideRepository::delete);
        log.info("Removed logistics override for PIN {}", pincode);
    }

    private LocalDate computeDeliveryDate(int transitDays) {
        LocalDate current = LocalDate.now();
        int daysAdded = 0;
        while (daysAdded < transitDays) {
            current = current.plusDays(1);
            if (current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                daysAdded++;
            }
        }
        return current;
    }

    private String formatEtaDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.equals(today.plusDays(1))) {
            return "Tomorrow (" + date.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)) + ")";
        }
        return date.format(DATE_FORMATTER);
    }
}
