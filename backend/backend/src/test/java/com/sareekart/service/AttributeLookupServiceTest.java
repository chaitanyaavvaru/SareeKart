package com.sareekart.service;

import com.sareekart.dto.response.LookupResponse;
import com.sareekart.entity.Color;
import com.sareekart.entity.Fabric;
import com.sareekart.entity.Occasion;
import com.sareekart.repository.ColorRepository;
import com.sareekart.repository.FabricRepository;
import com.sareekart.repository.OccasionRepository;
import com.sareekart.service.impl.AttributeLookupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AttributeLookupServiceTest {

    @Mock
    private FabricRepository fabricRepository;

    @Mock
    private OccasionRepository occasionRepository;

    @Mock
    private ColorRepository colorRepository;

    @InjectMocks
    private AttributeLookupServiceImpl lookupService;

    private Fabric fabric;
    private Occasion occasion;
    private Color color;

    @BeforeEach
    void setUp() {
        fabric = Fabric.builder()
                .id(1L)
                .name("Silk")
                .slug("silk")
                .careInstructions("Dry clean only")
                .active(true)
                .displayOrder(1)
                .build();

        occasion = Occasion.builder()
                .id(1L)
                .name("Wedding")
                .slug("wedding")
                .active(true)
                .displayOrder(1)
                .build();

        color = Color.builder()
                .id(1L)
                .name("Ruby Red")
                .slug("ruby-red")
                .family("Red")
                .hexCode("#C70039")
                .active(true)
                .displayOrder(1)
                .build();
    }

    @Test
    @DisplayName("Lookup: Returns active fabrics in display order")
    void getAllFabrics_ReturnsActiveFabrics() {
        when(fabricRepository.findByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(fabric));

        List<LookupResponse> result = lookupService.getAllFabrics(true);
        assertEquals(1, result.size());
        assertEquals("Silk", result.get(0).getName());
        assertEquals("Dry clean only", result.get(0).getCareInstructions());
    }

    @Test
    @DisplayName("Lookup: Returns active occasions in display order")
    void getAllOccasions_ReturnsActiveOccasions() {
        when(occasionRepository.findByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(occasion));

        List<LookupResponse> result = lookupService.getAllOccasions(true);
        assertEquals(1, result.size());
        assertEquals("Wedding", result.get(0).getName());
    }

    @Test
    @DisplayName("Lookup: Returns active colors with family and hexCode")
    void getAllColors_ReturnsActiveColorsWithHex() {
        when(colorRepository.findByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(color));

        List<LookupResponse> result = lookupService.getAllColors(true);
        assertEquals(1, result.size());
        assertEquals("Ruby Red", result.get(0).getName());
        assertEquals("Red", result.get(0).getFamily());
        assertEquals("#C70039", result.get(0).getHexCode());
    }
}
