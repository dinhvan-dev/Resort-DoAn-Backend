package com.example.resort.dto.response.report;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatusCountResponse {
    private String label;
    private Long count;
}
