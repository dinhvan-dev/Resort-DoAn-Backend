package com.example.resort.dto.response;

import com.example.resort.enums.cleaning.CleaningTaskStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CleaningTaskResponse {
    private Long taskId;
    private Long roomId;
    private String roomNumber;
    private Long bookingId;
    private String assignedTo;
    private CleaningTaskStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
}
