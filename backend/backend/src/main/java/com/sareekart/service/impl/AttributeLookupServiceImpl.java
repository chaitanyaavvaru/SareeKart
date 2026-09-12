package com.sareekart.service.impl;

import com.sareekart.dto.response.LookupResponse;
import com.sareekart.entity.Color;
import com.sareekart.entity.Fabric;
import com.sareekart.entity.Occasion;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.repository.ColorRepository;
import com.sareekart.repository.FabricRepository;
import com.sareekart.repository.OccasionRepository;
import com.sareekart.service.AttributeLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttributeLookupServiceImpl implements AttributeLookupService {

    private final FabricRepository fabricRepository;
    private final OccasionRepository occasionRepository;
    private final ColorRepository colorRepository;

    @Override
    public List<LookupResponse> getAllFabrics(boolean activeOnly) {
        List<Fabric> fabrics = activeOnly
                ? fabricRepository.findByActiveTrueOrderByDisplayOrderAsc()
                : fabricRepository.findAllByOrderByDisplayOrderAsc();

        return fabrics.stream()
                .map(f -> LookupResponse.builder()
                        .id(f.getId())
                        .name(f.getName())
                        .slug(f.getSlug())
                        .description(f.getDescription())
                        .careInstructions(f.getCareInstructions())
                        .displayOrder(f.getDisplayOrder())
                        .active(f.getActive())
                        .build())
                .toList();
    }

    @Override
    public List<LookupResponse> getAllOccasions(boolean activeOnly) {
        List<Occasion> occasions = activeOnly
                ? occasionRepository.findByActiveTrueOrderByDisplayOrderAsc()
                : occasionRepository.findAllByOrderByDisplayOrderAsc();

        return occasions.stream()
                .map(o -> LookupResponse.builder()
                        .id(o.getId())
                        .name(o.getName())
                        .slug(o.getSlug())
                        .description(o.getDescription())
                        .displayOrder(o.getDisplayOrder())
                        .active(o.getActive())
                        .build())
                .toList();
    }

    @Override
    public List<LookupResponse> getAllColors(boolean activeOnly) {
        List<Color> colors = activeOnly
                ? colorRepository.findByActiveTrueOrderByDisplayOrderAsc()
                : colorRepository.findAllByOrderByDisplayOrderAsc();

        return colors.stream()
                .map(c -> LookupResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .slug(c.getSlug())
                        .family(c.getFamily())
                        .hexCode(c.getHexCode())
                        .displayOrder(c.getDisplayOrder())
                        .active(c.getActive())
                        .build())
                .toList();
    }

    @Override
    public LookupResponse getFabricById(Long id) {
        Fabric f = fabricRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fabric", "id", id));
        return LookupResponse.builder()
                .id(f.getId())
                .name(f.getName())
                .slug(f.getSlug())
                .description(f.getDescription())
                .careInstructions(f.getCareInstructions())
                .displayOrder(f.getDisplayOrder())
                .active(f.getActive())
                .build();
    }

    @Override
    public LookupResponse getOccasionById(Long id) {
        Occasion o = occasionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Occasion", "id", id));
        return LookupResponse.builder()
                .id(o.getId())
                .name(o.getName())
                .slug(o.getSlug())
                .description(o.getDescription())
                .displayOrder(o.getDisplayOrder())
                .active(o.getActive())
                .build();
    }

    @Override
    public LookupResponse getColorById(Long id) {
        Color c = colorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Color", "id", id));
        return LookupResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .slug(c.getSlug())
                .family(c.getFamily())
                .hexCode(c.getHexCode())
                .displayOrder(c.getDisplayOrder())
                .active(c.getActive())
                .build();
    }
}
