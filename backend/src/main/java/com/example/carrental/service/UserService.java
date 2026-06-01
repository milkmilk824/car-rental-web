package com.example.carrental.service;

import com.example.carrental.common.BusinessException;
import com.example.carrental.common.Enums.UserRole;
import com.example.carrental.common.Enums.UserStatus;
import com.example.carrental.domain.User;
import com.example.carrental.dto.BootstrapDtos;
import com.example.carrental.dto.UserDtos;
import com.example.carrental.repository.UserRepository;
import com.example.carrental.security.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public UserDtos.LoginResponse register(UserDtos.RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw BusinessException.badRequest("用户名已存在");
        }
        if (request.phone() != null && !request.phone().isBlank() && userRepository.existsByPhone(request.phone())) {
            throw BusinessException.badRequest("手机号已被注册");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setEmail(request.email());
        userRepository.save(user);
        String token = tokenService.issue(user);
        return new UserDtos.LoginResponse(token, DtoMapper.toUserResponse(user));
    }

    @Transactional(readOnly = true)
    public UserDtos.LoginResponse login(UserDtos.LoginRequest request) {
        User user = findByUsername(request.username());
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw BusinessException.forbidden("账号已被禁用");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw BusinessException.badRequest("用户名或密码错误");
        }
        String token = tokenService.issue(user);
        return new UserDtos.LoginResponse(token, DtoMapper.toUserResponse(user));
    }

    @Transactional(readOnly = true)
    public UserDtos.UserResponse profile(Long userId) {
        return DtoMapper.toUserResponse(findById(userId));
    }

    public UserDtos.UserResponse updateProfile(Long userId, UserDtos.UpdateProfileRequest request) {
        User user = findById(userId);
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.realName() != null) {
            user.setRealName(request.realName());
        }
        return DtoMapper.toUserResponse(user);
    }

    public UserDtos.UserResponse updateLicense(Long userId, UserDtos.LicenseRequest request) {
        User user = findById(userId);
        user.setRealName(request.realName());
        user.setIdCard(request.idCard());
        user.setDriverLicenseNo(request.driverLicenseNo());
        return DtoMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserDtos.UserResponse> listUsers() {
        return userRepository.findAll().stream().map(DtoMapper::toUserResponse).toList();
    }

    public UserDtos.UserResponse updateStatus(Long userId, UserStatus status) {
        User user = findById(userId);
        user.setStatus(status);
        return DtoMapper.toUserResponse(user);
    }

    public UserDtos.UserResponse createUser(UserDtos.AdminCreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw BusinessException.badRequest("用户名已存在");
        }
        if (request.phone() != null && !request.phone().isBlank() && userRepository.existsByPhone(request.phone())) {
            throw BusinessException.badRequest("手机号已被注册");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setRole(request.role() != null ? request.role() : com.example.carrental.common.Enums.UserRole.USER);
        user.setStatus(request.status() != null ? request.status() : com.example.carrental.common.Enums.UserStatus.ACTIVE);
        userRepository.save(user);
        return DtoMapper.toUserResponse(user);
    }

    public UserDtos.LoginResponse bootstrapAdmin(BootstrapDtos.BootstrapAdminRequest request, String expectedSecret) {
        if (expectedSecret == null || expectedSecret.isBlank() || !expectedSecret.equals(request.secret())) {
            throw BusinessException.forbidden("Bootstrap 密钥错误");
        }
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            throw BusinessException.badRequest("管理员账号已存在");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw BusinessException.badRequest("用户名已存在");
        }
        if (request.phone() != null && !request.phone().isBlank() && userRepository.existsByPhone(request.phone())) {
            throw BusinessException.badRequest("手机号已被注册");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setRole(UserRole.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        return new UserDtos.LoginResponse(tokenService.issue(user), DtoMapper.toUserResponse(user));
    }

    public UserDtos.UserResponse updateUser(Long userId, UserDtos.AdminUpdateUserRequest request) {
        User user = findById(userId);
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.realName() != null) {
            user.setRealName(request.realName());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        return DtoMapper.toUserResponse(user);
    }

    public void deleteUser(Long userId) {
        User user = findById(userId);
        userRepository.delete(user);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> BusinessException.notFound("用户不存在"));
    }

    private User findByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> BusinessException.badRequest("用户名或密码错误"));
    }
}
