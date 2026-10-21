package com.sareekart.controller;

import com.sareekart.entity.Role;
import com.sareekart.entity.User;
import com.sareekart.repository.ReturnRequestRepository;
import com.sareekart.service.storage.FileStorageService;
import com.sareekart.service.storage.MediaMetadata;
import com.sareekart.service.storage.StorageCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MediaServingControllerTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @InjectMocks
    private MediaServingController mediaServingController;

    private MockMvc mockMvc;

    private User customerOwner;
    private User customerOther;
    private User adminUser;

    @BeforeEach
    void setUp() {
        customerOwner = User.builder().id(101L).email("owner@example.com").role(Role.CUSTOMER).build();
        customerOther = User.builder().id(202L).email("intruder@example.com").role(Role.CUSTOMER).build();
        adminUser = User.builder().id(999L).email("admin@sareekart.com").role(Role.ADMIN).build();

        HandlerMethodArgumentResolver userResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().equals(User.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                String authHeader = webRequest.getHeader("X-Simulate-User");
                if ("owner".equalsIgnoreCase(authHeader)) {
                    return customerOwner;
                } else if ("intruder".equalsIgnoreCase(authHeader)) {
                    return customerOther;
                } else if ("admin".equalsIgnoreCase(authHeader)) {
                    return adminUser;
                }
                return null; // Unauthenticated
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(mediaServingController)
                .setCustomArgumentResolvers(userResolver)
                .build();
    }

    @Test
    void serveMedia_sareePhoto_unauthenticated_returns200WithPublicCacheHeaders() throws Exception {
        byte[] imageBytes = "public saree image binary".getBytes(StandardCharsets.UTF_8);
        MediaMetadata metadata = MediaMetadata.builder()
                .contentType("image/jpeg")
                .contentLength((long) imageBytes.length)
                .storageClass("STANDARD")
                .eTag("\"etag-saree-1\"")
                .isArchived(false)
                .build();

        when(fileStorageService.getMetadata(StorageCategory.SAREE_PHOTOS, "kanchipuram.jpg"))
                .thenReturn(metadata);
        when(fileStorageService.openStream(StorageCategory.SAREE_PHOTOS, "kanchipuram.jpg"))
                .thenReturn(new ByteArrayInputStream(imageBytes));

        MvcResult mvcResult = mockMvc.perform(get("/uploads/saree-photos/kanchipuram.jpg"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=86400, immutable"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/jpeg"))
                .andExpect(header().string(HttpHeaders.ETAG, "\"etag-saree-1\""))
                .andExpect(content().bytes(imageBytes));
    }

    @Test
    void serveMedia_returnPhoto_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/uploads/return-photos/defect-1.jpg"))
                .andExpect(status().isUnauthorized());

        verify(fileStorageService, never()).getMetadata(any(), any());
        verify(fileStorageService, never()).openStream(any(), any());
    }

    @Test
    void serveMedia_returnPhoto_unauthorizedCustomer_returns403() throws Exception {
        when(returnRequestRepository.existsByUserIdAndImageFilename(customerOther.getId(), "defect-1.jpg"))
                .thenReturn(false);

        mockMvc.perform(get("/uploads/return-photos/defect-1.jpg")
                        .header("X-Simulate-User", "intruder"))
                .andExpect(status().isForbidden());

        verify(fileStorageService, never()).getMetadata(any(), any());
    }

    @Test
    void serveMedia_returnPhoto_authorizedCustomerOwner_returns200WithPrivateCacheHeaders() throws Exception {
        byte[] defectBytes = "customer defect photo data".getBytes(StandardCharsets.UTF_8);
        MediaMetadata metadata = MediaMetadata.builder()
                .contentType("image/png")
                .contentLength((long) defectBytes.length)
                .storageClass("STANDARD_IA")
                .eTag("\"defect-etag-2\"")
                .isArchived(false)
                .build();

        when(returnRequestRepository.existsByUserIdAndImageFilename(customerOwner.getId(), "defect-1.png"))
                .thenReturn(true);
        when(fileStorageService.getMetadata(StorageCategory.RETURN_PHOTOS, "defect-1.png"))
                .thenReturn(metadata);
        when(fileStorageService.openStream(StorageCategory.RETURN_PHOTOS, "defect-1.png"))
                .thenReturn(new ByteArrayInputStream(defectBytes));

        MvcResult mvcResult = mockMvc.perform(get("/uploads/return-photos/defect-1.png")
                        .header("X-Simulate-User", "owner"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-cache, no-store, must-revalidate"))
                .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
                .andExpect(header().string(HttpHeaders.EXPIRES, "0"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andExpect(content().bytes(defectBytes));
    }

    @Test
    void serveMedia_returnPhoto_adminStaff_bypassesOwnershipCheck_returns200() throws Exception {
        byte[] defectBytes = "admin inspect photo".getBytes(StandardCharsets.UTF_8);
        MediaMetadata metadata = MediaMetadata.builder()
                .contentType("image/jpeg")
                .contentLength((long) defectBytes.length)
                .storageClass("STANDARD")
                .isArchived(false)
                .build();

        when(fileStorageService.getMetadata(StorageCategory.RETURN_PHOTOS, "defect-admin.jpg"))
                .thenReturn(metadata);
        when(fileStorageService.openStream(StorageCategory.RETURN_PHOTOS, "defect-admin.jpg"))
                .thenReturn(new ByteArrayInputStream(defectBytes));

        MvcResult mvcResult = mockMvc.perform(get("/uploads/return-photos/defect-admin.jpg")
                        .header("X-Simulate-User", "admin"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-cache, no-store, must-revalidate"))
                .andExpect(content().bytes(defectBytes));

        // Staff does not require customer ownership lookup
        verify(returnRequestRepository, never()).existsByUserIdAndImageFilename(any(), any());
    }

    @Test
    void serveMedia_invalidCategory_returns400() throws Exception {
        mockMvc.perform(get("/uploads/passwords/root.txt"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void serveMedia_archivedColdStorage_returns409Conflict() throws Exception {
        MediaMetadata archivedMeta = MediaMetadata.builder()
                .contentType("image/jpeg")
                .contentLength(100L)
                .storageClass("GLACIER")
                .isArchived(true)
                .build();

        when(fileStorageService.getMetadata(StorageCategory.SAREE_PHOTOS, "old.jpg"))
                .thenReturn(archivedMeta);

        mockMvc.perform(get("/uploads/saree-photos/old.jpg"))
                .andExpect(status().isConflict());

        verify(fileStorageService, never()).openStream(any(), any());
    }

    @Test
    void serveMedia_notFoundInStorage_returns404() throws Exception {
        when(fileStorageService.getMetadata(StorageCategory.SAREE_PHOTOS, "missing.jpg"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        mockMvc.perform(get("/uploads/saree-photos/missing.jpg"))
                .andExpect(status().isNotFound());
    }
}
