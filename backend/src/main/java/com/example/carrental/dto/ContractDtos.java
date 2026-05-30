package com.example.carrental.dto;

import com.example.carrental.common.Enums.ContractStatus;

public final class ContractDtos {

    private ContractDtos() {
    }

    public record GenerateContractRequest(Long orderId) {
    }

    public record ContractResponse(
            Long id,
            String contractNo,
            Long orderId,
            Long userId,
            String contractUrl,
            ContractStatus signStatus
    ) {
    }
}
