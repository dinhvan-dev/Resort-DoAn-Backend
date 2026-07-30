package com.example.resort.dto.response.report;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TopRoomResponse {
    private String roomNumber;
    private Long bookingCount;
    private Double revenue;
}
