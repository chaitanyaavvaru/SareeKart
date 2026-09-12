package com.sareekart.service;

import com.sareekart.dto.request.ConsultationQuizRequest;
import com.sareekart.dto.request.DrapeStyleRequest;
import com.sareekart.dto.response.AiStylistTelemetryResponse;
import com.sareekart.dto.response.DrapeStyleResponse;
import com.sareekart.dto.response.ProductResponse;
import com.sareekart.entity.AiStyleConsultation;
import com.sareekart.entity.Product;
import com.sareekart.entity.User;
import com.sareekart.mapper.ProductMapper;
import com.sareekart.repository.AiStyleConsultationRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.impl.AiStylistServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AiStylistServiceImplTest {

    @Mock
    private AiStyleConsultationRepository consultationRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private AiStylistServiceImpl aiStylistService;

    private User testUser;
    private Product testSaree;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .firstName("Ananya")
                .lastName("Iyer")
                .email("ananya@example.com")
                .build();

        testSaree = Product.builder()
                .id(101L)
                .name("Kanchipuram Crimson Royal Bridal Silk Saree")
                .fabric("Pure Kanchipuram Mulberry Silk")
                .color("Crimson Red")
                .price(BigDecimal.valueOf(28500))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Red / Maroon saree generates 3 curated ensembles with Peacock Emerald Green contrast blouse")
    void testGenerateDrapeStyling_KanchipuramRedSilk_Generates3Ensembles() {
        when(productRepository.findById(101L)).thenReturn(Optional.of(testSaree));

        AiStyleConsultation savedConsultation = AiStyleConsultation.builder()
                .id(1L)
                .sareeName(testSaree.getName())
                .fabric(testSaree.getFabric())
                .primaryColor("Crimson Red")
                .occasion("Bridal")
                .convertedToTailoring(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(consultationRepository.save(any(AiStyleConsultation.class))).thenReturn(savedConsultation);

        DrapeStyleRequest req = DrapeStyleRequest.builder()
                .productId(101L)
                .sareeName(testSaree.getName())
                .fabric(testSaree.getFabric())
                .primaryColor("Crimson Red")
                .occasion("Bridal")
                .build();

        DrapeStyleResponse response = aiStylistService.generateDrapeStyling(req, testUser);

        assertNotNull(response);
        assertEquals(1L, response.getConsultationId());
        assertEquals("Kanchipuram Crimson Royal Bridal Silk Saree", response.getSareeName());
        assertNotNull(response.getCuratedLooks());
        assertEquals(3, response.getCuratedLooks().size());

        // Verify Look 1 (Heritage)
        DrapeStyleResponse.EnsembleLook look1 = response.getCuratedLooks().get(0);
        assertEquals("look-heritage", look1.getId());
        assertEquals("Royal Heritage Grandeur", look1.getTitle());
        assertEquals("Peacock Emerald Green", look1.getBlouse().getContrastColor());
        assertEquals("Sweetheart", look1.getBlouse().getFrontNeck());
        assertEquals("Elbow Length (Traditional)", look1.getBlouse().getSleeve());
        assertEquals("Temple Nakshi Antique Gold", look1.getJewelry().getCategory());

        // Verify Look 2 (Minimalist)
        DrapeStyleResponse.EnsembleLook look2 = response.getCuratedLooks().get(1);
        assertEquals("look-minimalist", look2.getId());
        assertEquals("Contemporary Minimalist Chic", look2.getTitle());
        assertEquals("Boat Neck", look2.getBlouse().getFrontNeck());
        assertEquals("Sleeveless", look2.getBlouse().getSleeve());

        // Verify Look 3 (Festive)
        DrapeStyleResponse.EnsembleLook look3 = response.getCuratedLooks().get(2);
        assertEquals("look-festive", look3.getId());
        assertEquals("Festive Fusion Drama", look3.getTitle());
        assertEquals("Deep U", look3.getBlouse().getFrontNeck());

        verify(consultationRepository, times(1)).save(any(AiStyleConsultation.class));
    }

    @Test
    @DisplayName("Yellow / Gold saree receives Royal Violet contrast blouse styling")
    void testGenerateDrapeStyling_YellowGold_ProvidesVioletContrast() {
        AiStyleConsultation saved = AiStyleConsultation.builder()
                .id(2L)
                .sareeName("Varanasi Pitambari Gold Brocade Saree")
                .primaryColor("Mustard Gold")
                .build();
        when(consultationRepository.save(any(AiStyleConsultation.class))).thenReturn(saved);

        DrapeStyleRequest req = DrapeStyleRequest.builder()
                .sareeName("Varanasi Pitambari Gold Brocade Saree")
                .fabric("Banarasi Katan Silk")
                .primaryColor("Mustard Gold")
                .occasion("Sangeet")
                .build();

        DrapeStyleResponse response = aiStylistService.generateDrapeStyling(req, null);

        assertNotNull(response);
        DrapeStyleResponse.EnsembleLook look1 = response.getCuratedLooks().get(0);
        assertEquals("Deep Royal Violet", look1.getBlouse().getContrastColor());
    }

    @Test
    @DisplayName("Blue / Teal saree receives Sunset Tangerine Orange contrast blouse styling")
    void testGenerateDrapeStyling_BlueTeal_ProvidesTangerineContrast() {
        AiStyleConsultation saved = AiStyleConsultation.builder()
                .id(3L)
                .sareeName("Kanchipuram Peacock Blue Korvai Saree")
                .primaryColor("Royal Blue")
                .build();
        when(consultationRepository.save(any(AiStyleConsultation.class))).thenReturn(saved);

        DrapeStyleRequest req = DrapeStyleRequest.builder()
                .sareeName("Kanchipuram Peacock Blue Korvai Saree")
                .fabric("Pure Silk")
                .primaryColor("Royal Blue")
                .occasion("Reception")
                .build();

        DrapeStyleResponse response = aiStylistService.generateDrapeStyling(req, null);

        assertNotNull(response);
        DrapeStyleResponse.EnsembleLook look1 = response.getCuratedLooks().get(0);
        assertEquals("Sunset Tangerine Orange", look1.getBlouse().getContrastColor());
    }

    @Test
    @DisplayName("Style quiz matches active products according to weave and budget criteria")
    void testConsultStyleQuiz_MatchesOccasionAndWeave() {
        List<Product> products = new ArrayList<>();
        products.add(testSaree);

        Product paithani = Product.builder()
                .id(102L)
                .name("Yeola Paithani Peacock Saree")
                .fabric("Pure Silk")
                .price(BigDecimal.valueOf(18000))
                .active(true)
                .build();
        products.add(paithani);

        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(new PageImpl<>(products));

        ProductResponse pResp = ProductResponse.builder()
                .id(101L)
                .name(testSaree.getName())
                .price(testSaree.getPrice())
                .build();
        when(productMapper.toResponse(any(Product.class))).thenReturn(pResp);

        ConsultationQuizRequest req = ConsultationQuizRequest.builder()
                .occasion("Wedding")
                .skinUndertone("WARM")
                .preferredWeave("KANCHIPURAM")
                .budgetRange("15K_TO_30K")
                .build();

        List<ProductResponse> matches = aiStylistService.consultStyleQuiz(req);

        assertNotNull(matches);
        assertFalse(matches.isEmpty());
        verify(productRepository, times(1)).findByActiveTrue(any(Pageable.class));
    }

    @Test
    @DisplayName("Recording tailoring conversion marks consultation as converted")
    void testRecordTailoringConversion_UpdatesEntityStatus() {
        AiStyleConsultation consultation = AiStyleConsultation.builder()
                .id(10L)
                .sareeName("Bridal Silk")
                .convertedToTailoring(false)
                .build();

        when(consultationRepository.findById(10L)).thenReturn(Optional.of(consultation));
        when(consultationRepository.save(any(AiStyleConsultation.class))).thenReturn(consultation);

        aiStylistService.recordTailoringConversion(10L);

        assertTrue(consultation.getConvertedToTailoring());
        verify(consultationRepository, times(1)).save(consultation);
    }

    @Test
    @DisplayName("Admin telemetry computes total consultations and tailoring conversion rate")
    void testGetTelemetry_CalculatesConversionAccurately() {
        when(consultationRepository.count()).thenReturn(100L);
        when(consultationRepository.countByConvertedToTailoringTrue()).thenReturn(35L);

        List<Object[]> occasions = new ArrayList<>();
        occasions.add(new Object[]{"Wedding", 60L});
        occasions.add(new Object[]{"Festive", 40L});
        when(consultationRepository.findOccasionFrequencies()).thenReturn(occasions);

        List<Object[]> topSarees = new ArrayList<>();
        topSarees.add(new Object[]{"Kanchipuram Bridal", 50L});
        when(consultationRepository.findTopStyledSarees()).thenReturn(topSarees);

        when(consultationRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(new ArrayList<>());

        AiStylistTelemetryResponse telemetry = aiStylistService.getTelemetry();

        assertNotNull(telemetry);
        assertEquals(100L, telemetry.getTotalConsultations());
        assertEquals(35L, telemetry.getConvertedToTailoringCount());
        assertEquals(35.0, telemetry.getTailoringConversionRatePercent());
        assertEquals(2, telemetry.getTopOccasions().size());
        assertEquals(1, telemetry.getTopStyledSarees().size());
    }
}
