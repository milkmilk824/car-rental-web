package com.example.carrental.service;

import com.example.carrental.common.BusinessException;
import com.example.carrental.common.Enums.StoreStatus;
import com.example.carrental.domain.Store;
import com.example.carrental.dto.StoreDtos;
import com.fasterxml.jackson.core.type.TypeReference;
import com.example.carrental.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;
    private final HotCacheService cacheService;

    public StoreService(StoreRepository storeRepository, HotCacheService cacheService) {
        this.storeRepository = storeRepository;
        this.cacheService = cacheService;
    }

    @Transactional(readOnly = true)
    public List<StoreDtos.StoreResponse> list(String city, Boolean onlyOpen) {
        String cacheKey = "store:list:" + (city == null ? "" : city.trim().toLowerCase()) + ":" + Boolean.TRUE.equals(onlyOpen);
        return cacheService.getOrLoad(
                cacheKey,
                Duration.ofMinutes(30),
                new TypeReference<>() {
                },
                () -> {
                    List<Store> stores;
                    if (city != null && !city.isBlank()) {
                        stores = storeRepository.findByCityContainingIgnoreCase(city);
                    } else if (Boolean.TRUE.equals(onlyOpen)) {
                        stores = storeRepository.findByStatus(StoreStatus.OPEN);
                    } else {
                        stores = storeRepository.findAll();
                    }
                    return stores.stream().map(DtoMapper::toStoreResponse).toList();
                }
        );
    }

    @Transactional(readOnly = true)
    public StoreDtos.StoreResponse detail(Long id) {
        return DtoMapper.toStoreResponse(findById(id));
    }

    public StoreDtos.StoreResponse create(StoreDtos.StoreRequest request) {
        Store store = new Store();
        apply(store, request);
        storeRepository.save(store);
        evictStoreCaches();
        return DtoMapper.toStoreResponse(store);
    }

    public StoreDtos.StoreResponse update(Long id, StoreDtos.StoreRequest request) {
        Store store = findById(id);
        apply(store, request);
        evictStoreCaches();
        return DtoMapper.toStoreResponse(store);
    }

    public void delete(Long id) {
        Store store = findById(id);
        storeRepository.delete(store);
        evictStoreCaches();
    }

    public Store findById(Long id) {
        return storeRepository.findById(id).orElseThrow(() -> BusinessException.notFound("门店不存在"));
    }

    private void apply(Store store, StoreDtos.StoreRequest request) {
        store.setStoreName(request.storeName());
        store.setCity(request.city());
        store.setAddress(request.address());
        store.setPhone(request.phone());
        store.setBusinessHours(request.businessHours());
        store.setStatus(request.status());
    }

    private void evictStoreCaches() {
        cacheService.evictPrefix("store:");
        cacheService.evictPrefix("stats:");
    }
}
