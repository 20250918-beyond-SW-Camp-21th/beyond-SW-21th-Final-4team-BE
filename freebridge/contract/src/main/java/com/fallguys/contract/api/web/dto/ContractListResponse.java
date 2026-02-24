package com.fallguys.contract.api.web.dto;

import com.fallguys.contract.api.web.PaginationInfo;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ContractListResponse {

    private List<ContractSummary> items;
    private PaginationInfo pagination;
}