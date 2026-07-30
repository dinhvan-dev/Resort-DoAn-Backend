package com.example.resort.service;

import com.example.resort.dto.response.CleaningTaskResponse;
import com.example.resort.entity.Booking;
import com.example.resort.entity.CleaningTask;
import com.example.resort.entity.room.Room;
import com.example.resort.enums.cleaning.CleaningTaskStatus;
import com.example.resort.enums.rooms.RoomStatus;
import com.example.resort.exception.AppException;
import com.example.resort.exception.ErrorCode;
import com.example.resort.repository.CleaningTaskRepository;
import com.example.resort.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CleaningTaskService {
    private static final Set<CleaningTaskStatus> OPEN_STATUSES =
            Set.of(CleaningTaskStatus.PENDING, CleaningTaskStatus.IN_PROGRESS, CleaningTaskStatus.DONE);

    private final CleaningTaskRepository cleaningTaskRepository;
    private final RoomRepository roomRepository;

    @Transactional
    public CleaningTaskResponse createTaskForCheckout(Room room, Booking booking) {
        if (cleaningTaskRepository.existsOpenTaskForRoom(room.getRoomId(), OPEN_STATUSES)) {
            throw new AppException(ErrorCode.CLEANING_TASK_ALREADY_EXISTS);
        }

        CleaningTask task = CleaningTask.builder()
                .room(room)
                .booking(booking)
                .status(CleaningTaskStatus.PENDING)
                .build();

        return toResponse(cleaningTaskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<CleaningTaskResponse> getAllTasks() {
        return cleaningTaskRepository.findAllActive()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CleaningTaskResponse> getMyTasks() {
        return cleaningTaskRepository.findWorkQueueForUser(
                        currentUsername(),
                        Set.of(CleaningTaskStatus.PENDING, CleaningTaskStatus.IN_PROGRESS)
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CleaningTaskResponse startTask(Long taskId) {
        CleaningTask task = findTaskForUpdate(taskId);
        if (task.getStatus() != CleaningTaskStatus.PENDING) {
            throw new AppException(ErrorCode.CLEANING_TASK_INVALID_STATUS);
        }

        task.setAssignedTo(currentUsername());
        task.setStatus(CleaningTaskStatus.IN_PROGRESS);
        task.setStartedAt(LocalDateTime.now());
        task.getRoom().setStatus(RoomStatus.CLEANING);
        roomRepository.save(task.getRoom());

        return toResponse(cleaningTaskRepository.save(task));
    }

    @Transactional
    public CleaningTaskResponse completeTask(Long taskId) {
        CleaningTask task = findTaskForUpdate(taskId);
        if (task.getStatus() != CleaningTaskStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.CLEANING_TASK_INVALID_STATUS);
        }
        if (task.getAssignedTo() != null && !task.getAssignedTo().equals(currentUsername())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        task.setStatus(CleaningTaskStatus.DONE);
        task.setCompletedAt(LocalDateTime.now());
        task.getRoom().setStatus(RoomStatus.NEEDS_CLEANING);
        roomRepository.save(task.getRoom());

        return toResponse(cleaningTaskRepository.save(task));
    }

    @Transactional
    public CleaningTaskResponse verifyTask(Long taskId) {
        CleaningTask task = findTaskForUpdate(taskId);
        if (task.getStatus() != CleaningTaskStatus.DONE) {
            throw new AppException(ErrorCode.CLEANING_TASK_INVALID_STATUS);
        }

        task.setStatus(CleaningTaskStatus.VERIFIED);
        task.setVerifiedAt(LocalDateTime.now());
        task.getRoom().setStatus(RoomStatus.AVAILABLE);
        roomRepository.save(task.getRoom());

        return toResponse(cleaningTaskRepository.save(task));
    }

    private CleaningTask findTask(Long taskId) {
        return cleaningTaskRepository.findActiveById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.CLEANING_TASK_NOT_FOUND));
    }

    private CleaningTask findTaskForUpdate(Long taskId) {
        return cleaningTaskRepository.findActiveByIdForUpdate(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.CLEANING_TASK_NOT_FOUND));
    }

    private CleaningTaskResponse toResponse(CleaningTask task) {
        return CleaningTaskResponse.builder()
                .taskId(task.getTaskId())
                .roomId(task.getRoom().getRoomId())
                .roomNumber(task.getRoom().getRoomNumber())
                .bookingId(task.getBooking() == null ? null : task.getBooking().getBookingId())
                .assignedTo(task.getAssignedTo())
                .status(task.getStatus())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .verifiedAt(task.getVerifiedAt())
                .createdAt(task.getCreatedAt())
                .build();
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
