package com.example.carrental.service;

import com.example.carrental.common.BusinessException;
import com.example.carrental.common.Enums.UserRole;
import com.example.carrental.domain.Store;
import com.example.carrental.domain.StoreStaff;
import com.example.carrental.domain.User;
import com.example.carrental.dto.StoreDtos;
import com.example.carrental.repository.StoreStaffRepository;
import com.example.carrental.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class StoreStaffService {

    private final StoreStaffRepository storeStaffRepository;
    private final StoreService storeService;
    private final UserService userService;

    public StoreStaffService(StoreStaffRepository storeStaffRepository, StoreService storeService, UserService userService) {
        this.storeStaffRepository = storeStaffRepository;
        this.storeService = storeService;
        this.userService = userService;
    }

    public StoreDtos.StoreStaffResponse bind(Long storeId, Long userId) {
        Store store = storeService.findById(storeId);
        User user = userService.findById(userId);
        if (user.getRole() != UserRole.STORE_STAFF) {
            user.setRole(UserRole.STORE_STAFF);
        }
        StoreStaff storeStaff = storeStaffRepository.findByUserIdAndStoreId(userId, storeId).orElseGet(() -> {
            StoreStaff created = new StoreStaff();
            created.setStore(store);
            created.setUser(user);
            return storeStaffRepository.save(created);
        });
        return DtoMapper.toStoreStaffResponse(storeStaff);
    }

    public void unbind(Long storeId, Long userId) {
        StoreStaff storeStaff = storeStaffRepository.findByUserIdAndStoreId(userId, storeId)
                .orElseThrow(() -> BusinessException.notFound("门店员工绑定不存在"));
        storeStaffRepository.delete(storeStaff);
    }

    @Transactional(readOnly = true)
    public List<StoreDtos.StoreStaffResponse> staffByStore(Long storeId) {
        storeService.findById(storeId);
        return storeStaffRepository.findByStoreIdOrderByCreateTimeAsc(storeId).stream()
                .map(DtoMapper::toStoreStaffResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StoreDtos.StoreResponse> myStores(Long userId) {
        return storeStaffRepository.findByUserIdOrderByCreateTimeAsc(userId).stream()
                .map(StoreStaff::getStore)
                .map(DtoMapper::toStoreResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public void ensureStoreAccess(Long storeId, CurrentUser currentUser) {
        if (currentUser.role() == UserRole.ADMIN) {
            return;
        }
        if (!storeStaffRepository.existsByUserIdAndStoreId(currentUser.id(), storeId)) {
            throw BusinessException.forbidden("当前门店不属于该员工");
        }
    }
}
