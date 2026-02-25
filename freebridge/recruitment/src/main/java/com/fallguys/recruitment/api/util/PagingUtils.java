package com.fallguys.recruitment.api.util;

import com.fallguys.recruitment.api.dto.response.PagedResponseDTO;

import java.util.List;

public final class PagingUtils {

    private PagingUtils() {
    }

    public static <T> PagedResponseDTO<T> toPagedResponse(List<T> source, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        long sourceSize = source.size();
        long startLong = Math.min((long) safePage * (long) safeSize, sourceSize);
        int fromIndex = (int) startLong;
        long endLong = Math.min(startLong + (long) safeSize, sourceSize);
        int toIndex = (int) endLong;
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
