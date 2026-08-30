package com.sareekart.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
    private boolean first;
    private int numberOfElements;

    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return PageResponse.<T>builder()
            .content(page.getContent().stream().map(mapper).collect(Collectors.toList()))
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .last(page.isLast())
            .first(page.isFirst())
            .numberOfElements(page.getNumberOfElements())
            .build();
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return from(page, Function.identity());
    }
}