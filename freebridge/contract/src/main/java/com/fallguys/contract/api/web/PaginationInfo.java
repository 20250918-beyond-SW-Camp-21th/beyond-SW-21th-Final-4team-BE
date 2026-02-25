package com.fallguys.contract.api.web;

public record PaginationInfo(int page, int limit, long total, int totalPages) {}