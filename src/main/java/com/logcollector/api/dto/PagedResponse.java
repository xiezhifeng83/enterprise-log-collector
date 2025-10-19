package com.logcollector.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic paged response DTO.
 *
 * @param <T> the type of content
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int number;
    private int size;
    private boolean last;
    private boolean first;

    public static <T> PagedResponse<T> of(
            List<T> content,
            long totalElements,
            int totalPages,
            int pageNumber,
            int pageSize) {
        return new PagedResponse<>(
                content,
                totalElements,
                totalPages,
                pageNumber,
                pageSize,
                pageNumber >= totalPages - 1,
                pageNumber == 0
        );
    }
}
