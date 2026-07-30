package com.example.resort.controller;

import com.example.resort.dto.response.ApiResponse;
import com.example.resort.dto.response.report.AdminSummaryResponse;
import com.example.resort.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {
    private final AdminReportService adminReportService;

    @GetMapping("/summary")
    public ApiResponse<AdminSummaryResponse> getSummary()
    {
        return ApiResponse.<AdminSummaryResponse>builder()
                .result(adminReportService.getSummary())
                .build();
    }
}
