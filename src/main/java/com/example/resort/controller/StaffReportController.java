package com.example.resort.controller;

import com.example.resort.dto.response.ApiResponse;
import com.example.resort.dto.response.staff.StaffSummaryResponse;
import com.example.resort.service.StaffReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/staff/reports")
@RequiredArgsConstructor
public class StaffReportController {
    private final StaffReportService staffReportService;

    @GetMapping("/summary")
    public ApiResponse<StaffSummaryResponse> getSummary()
    {
        return ApiResponse.<StaffSummaryResponse>builder()
                .result(staffReportService.getSummary())
                .build();
    }
}
