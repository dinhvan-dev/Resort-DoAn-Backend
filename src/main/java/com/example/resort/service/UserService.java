package com.example.resort.service;

import com.example.resort.aop.logging.Auditable;
import com.example.resort.dto.request.UserCreateRequest;
import com.example.resort.dto.request.UserPasswordChangeRequest;
import com.example.resort.dto.request.UserRoleUpdateRequest;
import com.example.resort.dto.request.UserSelfUpdateRequest;
import com.example.resort.dto.request.UserUpdateRequest;
import com.example.resort.dto.response.UserResponse;
import com.example.resort.entity.User;
import com.example.resort.enums.Role;
import com.example.resort.exception.AppException;
import com.example.resort.exception.ErrorCode;
import com.example.resort.mapper.UserMapper;
import com.example.resort.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @Auditable(
            action = "CREATE",
            entity = "User",
            entityId = "#result.userId",
            detail = "'Created user ' + #result.username + ' role ' + #result.role"
    )
    public UserResponse createUser(UserCreateRequest request) {
        return createUserWithRole(request, Role.USER);
    }

    @Transactional
    @Auditable(
            action = "CREATE",
            entity = "User",
            entityId = "#result.userId",
            detail = "'Created staff account ' + #result.username"
    )
    public UserResponse createStaff(UserCreateRequest request) {
        return createUserWithRole(request, Role.STAFF);
    }

    @Transactional
    @Auditable(
            action = "CREATE",
            entity = "User",
            entityId = "#result.userId",
            detail = "'Created housekeeping account ' + #result.username"
    )
    public UserResponse createHousekeeping(UserCreateRequest request) {
        return createUserWithRole(request, Role.HOUSEKEEPING);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAllActive()
                .stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllStaff() {
        return getUsersByRole(Role.STAFF);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllHousekeeping() {
        return getUsersByRole(Role.HOUSEKEEPING);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(String userId) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findActiveByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRole(Role role) {
        return userRepository.findActiveByRole(role)
                .stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    @Transactional
    @Auditable(
            action = "UPDATE",
            entity = "User",
            entityId = "#result.userId",
            detail = "'Updated user ' + #result.username"
    )
    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        validateUpdateUser(user, userId, request);
        userMapper.updateUser(user, request);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    @Auditable(
            action = "UPDATE_SELF",
            entity = "User",
            entityId = "#result.userId",
            detail = "'Updated own account profile for ' + #result.username"
    )
    public UserResponse updateMyInfo(UserSelfUpdateRequest request) {
        User user = findCurrentUser();
        validateSelfUpdateUser(user, request);

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    @Auditable(
            action = "CHANGE_PASSWORD",
            entity = "User",
            entityId = "#result.userId",
            detail = "'Changed own account password for ' + #result.username"
    )
    public UserResponse changeMyPassword(UserPasswordChangeRequest request) {
        User user = findCurrentUser();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    @Auditable(
            action = "UPDATE_ROLE",
            entity = "User",
            entityId = "#result.userId",
            detail = "'Updated user role for ' + #result.username + ' to ' + #result.role"
    )
    public UserResponse updateUserRole(String userId, UserRoleUpdateRequest request) {
        if (request.getRole() == Role.ADMIN) {
            throw new AppException(ErrorCode.INVALID_USER_ROLE);
        }

        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (user.getRole() == Role.ADMIN) {
            throw new AppException(ErrorCode.INVALID_USER_ROLE);
        }

        user.setRole(request.getRole());
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    @Auditable(
            action = "DELETE",
            entity = "User",
            entityId = "#p0",
            detail = "'Deleted user ' + #p0"
    )
    public void deleteUser(String userId) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getMyInfo() {
        return userMapper.toUserResponse(findCurrentUser());
    }

    private UserResponse createUserWithRole(UserCreateRequest request, Role role) {
        validateCreateUser(request);

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);

        return userMapper.toUserResponse(userRepository.save(user));
    }

    private void validateCreateUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EMAIL_EXISTS);
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AppException(ErrorCode.USER_PHONE_EXISTS);
        }
    }

    private void validateUpdateUser(User user, String userId, UserUpdateRequest request) {
        if (request.getUsername() != null
                && !request.getUsername().equals(user.getUsername())
                && userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }
        if (request.getEmail() != null
                && !request.getEmail().equals(user.getEmail())
                && userRepository.existsByEmailAndUserIdNot(request.getEmail(), userId)) {
            throw new AppException(ErrorCode.USER_EMAIL_EXISTS);
        }
        if (request.getPhoneNumber() != null
                && !request.getPhoneNumber().equals(user.getPhoneNumber())
                && userRepository.existsByPhoneNumberAndUserIdNot(request.getPhoneNumber(), userId)) {
            throw new AppException(ErrorCode.USER_PHONE_EXISTS);
        }
    }

    private void validateSelfUpdateUser(User user, UserSelfUpdateRequest request) {
        if (!request.getEmail().equals(user.getEmail())
                && userRepository.existsByEmailAndUserIdNot(request.getEmail(), user.getUserId())) {
            throw new AppException(ErrorCode.USER_EMAIL_EXISTS);
        }
        if (!request.getPhoneNumber().equals(user.getPhoneNumber())
                && userRepository.existsByPhoneNumberAndUserIdNot(request.getPhoneNumber(), user.getUserId())) {
            throw new AppException(ErrorCode.USER_PHONE_EXISTS);
        }
    }

    private User findCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findActiveByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
