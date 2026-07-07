package com.example.resort.controller;

import com.example.resort.dto.response.ApiResponse;
import com.example.resort.dto.response.CleaningTaskResponse;
import com.example.resort.service.CleaningTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cleaning-tasks")
@RequiredArgsConstructor
public class CleaningTaskController {
    private final CleaningTaskService cleaningTaskService;

    @GetMapping
    public ApiResponse<List<CleaningTaskResponse>> getAllTasks() {
        return ApiResponse.<List<CleaningTaskResponse>>builder()
                .result(cleaningTaskService.getAllTasks())
                .build();
    }

    @GetMapping("/my-tasks")
    public ApiResponse<List<CleaningTaskResponse>> getMyTasks() {
        return ApiResponse.<List<CleaningTaskResponse>>builder()
                .result(cleaningTaskService.getMyTasks())
                .build();
    }

    @PatchMapping("/{taskId}/start")
    public ApiResponse<CleaningTaskResponse> startTask(@PathVariable Long taskId) {
        return ApiResponse.<CleaningTaskResponse>builder()
                .result(cleaningTaskService.startTask(taskId))
                .build();
    }

    @PatchMapping("/{taskId}/complete")
    public ApiResponse<CleaningTaskResponse> completeTask(@PathVariable Long taskId) {
        return ApiResponse.<CleaningTaskResponse>builder()
                .result(cleaningTaskService.completeTask(taskId))
                .build();
    }

    @PatchMapping("/{taskId}/verify")
    public ApiResponse<CleaningTaskResponse> verifyTask(@PathVariable Long taskId) {
        return ApiResponse.<CleaningTaskResponse>builder()
                .result(cleaningTaskService.verifyTask(taskId))
                .build();
    }
}
