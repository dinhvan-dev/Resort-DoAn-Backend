package com.example.resort.repository;

import com.example.resort.entity.CleaningTask;
import com.example.resort.enums.cleaning.CleaningTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CleaningTaskRepository extends JpaRepository<CleaningTask, Long> {

    @Query("SELECT c FROM CleaningTask c WHERE c.taskId = :taskId AND c.isActive = true")
    Optional<CleaningTask> findActiveById(@Param("taskId") Long taskId);

    @Query("SELECT c FROM CleaningTask c WHERE c.isActive = true ORDER BY c.createdAt DESC")
    List<CleaningTask> findAllActive();

    @Query("""
            SELECT c FROM CleaningTask c
            WHERE c.isActive = true
            AND (c.assignedTo = :username OR c.assignedTo IS NULL)
            AND c.status IN :statuses
            ORDER BY c.createdAt DESC
            """)
    List<CleaningTask> findWorkQueueForUser(
            @Param("username") String username,
            @Param("statuses") Collection<CleaningTaskStatus> statuses
    );

    @Query("""
            SELECT COUNT(c) > 0 FROM CleaningTask c
            WHERE c.room.roomId = :roomId
            AND c.isActive = true
            AND c.status IN :statuses
            """)
    boolean existsOpenTaskForRoom(
            @Param("roomId") Long roomId,
            @Param("statuses") Collection<CleaningTaskStatus> statuses
    );
}
