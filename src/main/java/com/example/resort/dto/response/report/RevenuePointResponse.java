package com.example.resort.dto.response.report;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RevenuePointResponse {
    private LocalDate date;
    private Double amount;
}
