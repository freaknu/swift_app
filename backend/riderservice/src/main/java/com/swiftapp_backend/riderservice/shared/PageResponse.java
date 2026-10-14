package com.swiftapp_backend.riderservice.shared;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> of(
            List<T> content, int page, int size, long totalElements, int totalPages, boolean last) {
        return new PageResponse<>(content, page, size, totalElements, totalPages, last);
    }
}
