package com.sareekart.service;

import com.sareekart.dto.internal.StylistIntent;
import com.sareekart.dto.request.StylistChatRequest;
import com.sareekart.service.impl.StylistIntentExtractorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StylistIntentExtractor Unit Tests")
public class StylistIntentExtractorTest {

    private StylistIntentExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new StylistIntentExtractorImpl();
    }

    @Test
    @DisplayName("Should extract budget with 'under 15000' and 'under 15k'")
    void testExtractBudgetUnder() {
        StylistChatRequest req1 = StylistChatRequest.builder().message("Show me sarees under 15000").build();
        StylistIntent intent1 = extractor.extractIntent(req1.getMessage(), req1);
        assertEquals(new BigDecimal("15000"), intent1.getMaxPrice());

        StylistChatRequest req2 = StylistChatRequest.builder().message("I want wedding sarees under 25k").build();
        StylistIntent intent2 = extractor.extractIntent(req2.getMessage(), req2);
        assertEquals(new BigDecimal("25000"), intent2.getMaxPrice());
    }

    @Test
    @DisplayName("Should extract budget range with 'between 10k and 20k'")
    void testExtractBudgetBetween() {
        StylistChatRequest req = StylistChatRequest.builder().message("Looking for sarees between 10k and 20k").build();
        StylistIntent intent = extractor.extractIntent(req.getMessage(), req);
        assertEquals(new BigDecimal("10000"), intent.getMinPrice());
        assertEquals(new BigDecimal("20000"), intent.getMaxPrice());
    }

    @Test
    @DisplayName("Should extract cultural occasions: Wedding, Reception, Festive")
    void testExtractOccasions() {
        StylistIntent weddingIntent = extractor.extractIntent("Need a bridal saree for muhurtham", null);
        assertEquals("Wedding", weddingIntent.getOccasion());

        StylistIntent receptionIntent = extractor.extractIntent("Looking for sangeet & reception drape", null);
        assertEquals("Reception", receptionIntent.getOccasion());

        StylistIntent festiveIntent = extractor.extractIntent("Festive diwali wear", null);
        assertEquals("Festive", festiveIntent.getOccasion());
    }

    @Test
    @DisplayName("Should extract fabrics: Kanchipuram, Banarasi, Organza")
    void testExtractFabrics() {
        StylistIntent kanchiIntent = extractor.extractIntent("Show me royal kanchipuram silk sarees", null);
        assertEquals("Kanchipuram Silk", kanchiIntent.getPreferredFabric());

        StylistIntent banarasiIntent = extractor.extractIntent("Pure banarasi brocade for dinner", null);
        assertEquals("Banarasi Silk", banarasiIntent.getPreferredFabric());

        StylistIntent organzaIntent = extractor.extractIntent("Looking for tissue organza lightweight drape", null);
        assertEquals("Tissue Organza", organzaIntent.getPreferredFabric());
    }

    @Test
    @DisplayName("Should extract colors and canonical color families")
    void testExtractColorsAndFamilies() {
        StylistIntent maroonIntent = extractor.extractIntent("I want a rich maroon silk saree", null);
        assertEquals("maroon", maroonIntent.getPreferredColor());
        assertEquals("Red", maroonIntent.getColorFamily());

        StylistIntent emeraldIntent = extractor.extractIntent("Emerald green saree with gold zari", null);
        assertEquals("emerald", emeraldIntent.getPreferredColor());
        assertEquals("Green", emeraldIntent.getColorFamily());
    }

    @Test
    @DisplayName("Should detect query types: SIMILAR_CHEAPER, SIMILAR_LUXURY, BLOUSE_PAIRING, DRAPING_ADVICE")
    void testDetectQueryTypes() {
        StylistChatRequest cheaperReq = StylistChatRequest.builder()
                .message("Find something similar but cheaper")
                .referenceProductId(42L)
                .build();
        StylistIntent cheaperIntent = extractor.extractIntent(cheaperReq.getMessage(), cheaperReq);
        assertEquals(StylistIntent.QueryType.SIMILAR_CHEAPER, cheaperIntent.getQueryType());
        assertEquals(42L, cheaperIntent.getReferenceProductId());

        StylistIntent luxuryIntent = extractor.extractIntent("Show me a more luxurious heirloom alternative", null);
        assertEquals(StylistIntent.QueryType.SIMILAR_LUXURY, luxuryIntent.getQueryType());

        StylistIntent blouseIntent = extractor.extractIntent("What contrast blouse goes with this?", null);
        assertEquals(StylistIntent.QueryType.BLOUSE_PAIRING, blouseIntent.getQueryType());

        StylistIntent drapeIntent = extractor.extractIntent("How to drape floating pallu style?", null);
        assertEquals(StylistIntent.QueryType.DRAPING_ADVICE, drapeIntent.getQueryType());
    }

    @Test
    @DisplayName("Should fall back to request chips when message contains no filters")
    void testFallbackToRequestChips() {
        StylistChatRequest req = StylistChatRequest.builder()
                .message("Show me good options")
                .occasion("Wedding")
                .preferredWeave("BANARASI")
                .budgetRange("UNDER_15K")
                .skinUndertone("WARM")
                .build();

        StylistIntent intent = extractor.extractIntent(req.getMessage(), req);
        assertEquals("Wedding", intent.getOccasion());
        assertEquals("BANARASI", intent.getPreferredFabric());
        assertEquals(new BigDecimal("15000"), intent.getMaxPrice());
        assertEquals("WARM", intent.getSkinUndertone());
    }
}
