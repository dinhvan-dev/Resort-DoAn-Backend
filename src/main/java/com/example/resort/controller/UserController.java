package com.example.resort.controller;

import com.example.resort.dto.request.UserCreateRequest;
import com.example.resort.dto.request.UserPasswordChangeRequest;
import com.example.resort.dto.request.UserRoleUpdateRequest;
import com.example.resort.dto.request.UserSelfUpdateRequest;
import com.example.resort.dto.request.UserUpdateRequest;
import com.example.resort.dto.response.ApiResponse;
import com.example.resort.dto.response.UserResponse;
import com.example.resort.enums.Role;
import com.example.resort.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.createUser(request))
                .build();
    }

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> createStaff(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.createStaff(request))
                .build();
    }

    @PostMapping("/housekeeping")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> createHousekeeping(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.createHousekeeping(request))
                .build();
    }

    // GET /users — Lấy tất cả users
    @GetMapping("/staff")
    public ApiResponse<List<UserResponse>> getAllStaff() {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getAllStaff())
                .build();
    }

    @GetMapping("/housekeeping")
    public ApiResponse<List<UserResponse>> getAllHousekeeping() {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getAllHousekeeping())
                .build();
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> getAllUsers() {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getAllUsers())
                .build();
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateMyInfo(@Valid @RequestBody UserSelfUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateMyInfo(request))
                .build();
    }

    @PutMapping("/me/password")
    public ApiResponse<UserResponse> changeMyPassword(@Valid @RequestBody UserPasswordChangeRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.changeMyPassword(request))
                .build();
    }

    // GET /users/{userId} — Lấy user theo ID (đầy đủ nhất)
    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUserById(@PathVariable String userId) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUserById(userId))
                .build();
    }

    // GET /users/username/{username} — Lấy user theo username
    @GetMapping("/username/{username}")
    public ApiResponse<UserResponse> getUserByUsername(@PathVariable String username) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUserByUsername(username))
                .build();
    }

    // GET /users/role?role=ADMIN — Lấy danh sách user theo role
    @GetMapping("/role")
    public ApiResponse<List<UserResponse>> getUsersByRole(@RequestParam Role role) {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getUsersByRole(role))
                .build();
    }

    // PUT /users/{userId} — Cập nhật user
    @PutMapping("/{userId}")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUser(userId, request))
                .build();
    }

    // DELETE /users/{userId} — Xóa user
    @PatchMapping("/{userId}/role")
    public ApiResponse<UserResponse> updateUserRole(
            @PathVariable String userId,
            @Valid @RequestBody UserRoleUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUserRole(userId, request))
                .build();
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ApiResponse.<Void>builder()
                .build();
    }
    @GetMapping("/myinfo")
    ApiResponse<UserResponse> getMyInfo()
    {
        return ApiResponse.<UserResponse> builder()
                .result(userService.getMyInfo())
                .build();
    }
}
