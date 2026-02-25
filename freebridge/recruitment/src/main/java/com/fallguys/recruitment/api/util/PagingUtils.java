package com.fallguys.recruitment.api.util;

import com.fallguys.recruitment.api.dto.response.PagedResponseDTO;

import java.util.List;

public final class PagingUtils {

    private PagingUtils() {
    }

    public static <T> PagedResponseDTO<T> toPagedResponse(List<T> source, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int fromIndex = Math.min(safePage * safeSize, source.size());
        int toIndex = Math.min(fromIndex + safeSize, source.size());
        int totalPages = (int) Math.ceil((double) source.size() / safeSize);

        return new PagedResponseDTO<>(
                source.subList(fromIndex, toIndex),
                safePage,
                safeSize,
                source.size(),
                totalPages
        );
    }
}
