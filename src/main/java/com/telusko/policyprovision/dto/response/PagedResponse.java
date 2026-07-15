package com.telusko.policyprovision.dto.response;

import java.util.List;

/**
 * Generic pagination wrapper (bonus: pagination on list endpoints).
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PagedResponse<T> of(List<T> fullList, int page, int size) {
        int totalElements = fullList.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<T> pageContent = fullList.subList(fromIndex, toIndex);
        return new PagedResponse<>(pageContent, page, size, totalElements, totalPages);
    }
}
