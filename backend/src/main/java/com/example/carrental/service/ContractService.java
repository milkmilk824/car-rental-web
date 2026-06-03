package com.example.carrental.service;

import com.example.carrental.common.BusinessException;
import com.example.carrental.common.Enums.ContractStatus;
import com.example.carrental.common.Enums.OrderStatus;
import com.example.carrental.common.PageResult;
import com.example.carrental.domain.Contract;
import com.example.carrental.domain.RentalOrder;
import com.example.carrental.dto.ContractDtos;
import com.example.carrental.repository.ContractRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ContractService {

    private final ContractRepository contractRepository;
    private final OrderService orderService;

    public ContractService(ContractRepository contractRepository, OrderService orderService) {
        this.contractRepository = contractRepository;
        this.orderService = orderService;
    }

    public ContractDtos.ContractResponse generate(Long orderId) {
        return DtoMapper.toContractResponse(generateInternal(orderId));
    }

    @Transactional(readOnly = true)
    public PageResult<ContractDtos.ContractResponse> listAll(int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)));
        return PageResult.from(contractRepository.findAllByOrderByCreateTimeDesc(pageRequest).map(DtoMapper::toContractResponse));
    }

    public Contract generateInternal(Long orderId) {
        return contractRepository.findByRentalOrderId(orderId).orElseGet(() -> {
            RentalOrder order = orderService.findById(orderId);
            if (order.getStatus() == OrderStatus.PENDING_PAYMENT || order.getStatus() == OrderStatus.CANCELLED) {
                throw BusinessException.badRequest("订单未支付或已取消，不能生成合同");
            }
            Contract contract = new Contract();
            contract.setContractNo("CT" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
            contract.setRentalOrder(order);
            contract.setUser(order.getUser());
            contract.setContractUrl("/files/contracts/" + contract.getContractNo() + ".pdf");
            contract.setSignStatus(ContractStatus.UNSIGNED);
            return contractRepository.save(contract);
        });
    }

    @Transactional(readOnly = true)
    public ContractDtos.ContractResponse detail(Long contractId) {
        Contract contract = contractRepository.findById(contractId).orElseThrow(() -> BusinessException.notFound("合同不存在"));
        return DtoMapper.toContractResponse(contract);
    }

    @Transactional(readOnly = true)
    public ContractDtos.ContractResponse byOrder(Long orderId) {
        Contract contract = contractRepository.findByRentalOrderId(orderId).orElseThrow(() -> BusinessException.notFound("合同不存在"));
        return DtoMapper.toContractResponse(contract);
    }

    public ContractDtos.ContractResponse sign(Long contractId) {
        Contract contract = contractRepository.findById(contractId).orElseThrow(() -> BusinessException.notFound("合同不存在"));
        contract.setSignStatus(ContractStatus.SIGNED);
        return DtoMapper.toContractResponse(contract);
    }
}
