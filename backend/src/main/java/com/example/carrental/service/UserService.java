package com.example.carrental.service;

import com.example.carrental.common.BusinessException;
import com.example.carrental.common.Enums.UserRole;
import com.example.carrental.common.Enums.UserStatus;
import com.example.carrental.common.PageResult;
import com.example.carrental.domain.RefreshToken;
import com.example.carrental.domain.User;
import com.example.carrental.dto.BootstrapDtos;
import com.example.carrental.dto.UserDtos;
import com.example.carrental.repository.RefreshTokenRepository;
import com.example.carrental.repository.UserRepository;
import com.example.carrental.security.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final Duration refreshTokenTtl;

    public UserService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            @Value("${app.session.refresh-token-ttl:P14D}") Duration refreshTokenTtl
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshTokenTtl = refreshTokenTtl;
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
        return issueLoginResponse(user);
    }

    public UserDtos.LoginResponse login(UserDtos.LoginRequest request) {
        User user = findByUsername(request.username());
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw BusinessException.forbidden("账号已被禁用");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw BusinessException.badRequest("用户名或密码错误");
        }
        return issueLoginResponse(user);
    }

    public UserDtos.LoginResponse refresh(UserDtos.RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> BusinessException.unauthorized("刷新令牌无效"));
        LocalDateTime now = LocalDateTime.now();
        if (refreshToken.getRevokedAt() != null || !refreshToken.getExpiresAt().isAfter(now)) {
            throw BusinessException.unauthorized("刷新令牌已失效");
        }
        User user = refreshToken.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw BusinessException.forbidden("账号已被禁用");
        }
        refreshToken.setRevokedAt(now);
        return issueLoginResponse(user);
    }

    public void logout(String rawAuthorization, UserDtos.LogoutRequest request) {
        tokenService.revoke(rawAuthorization);
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            return;
        }
        refreshTokenRepository.findByToken(request.refreshToken())
                .ifPresent(refreshToken -> refreshToken.setRevokedAt(LocalDateTime.now()));
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
    public PageResult<UserDtos.UserResponse> listUsers(int page, int size) {
        return PageResult.from(userRepository.findAll(pageRequest(page, size)).map(DtoMapper::toUserResponse));
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
        return issueLoginResponse(user);
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

    private UserDtos.LoginResponse issueLoginResponse(User user) {
        return new UserDtos.LoginResponse(tokenService.issue(user), issueRefreshToken(user), DtoMapper.toUserResponse(user));
    }

    private String issueRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plus(refreshTokenTtl));
        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }

    private PageRequest pageRequest(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createTime"));
    }
}
