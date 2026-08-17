package com.edgareldy.springmicroservicestutorial.commonlib.dto;

import java.util.List;

/**
 * Generic envelope for a single page of results, used by every service that
 * exposes a paginated listing endpoint (e.g. {@code GET /api/v1/catalog/categories}),
 * so pagination metadata is shaped identically across the whole system.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public record PageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean last
) {

    /**
     * Builds a page response from raw page data, deriving {@code totalPages}
     * and {@code last} from the given element counts rather than requiring
     * callers to compute them by hand.
     */
    public static <T> PageResponse<T> of(List<T> content, int pageNumber, int pageSize, long totalElements) {
        int totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) totalElements / (double) pageSize);
        boolean last = pageNumber >= totalPages - 1;
        return new PageResponse<>(content, pageNumber, pageSize, totalElements, totalPages, last);
    }
}
