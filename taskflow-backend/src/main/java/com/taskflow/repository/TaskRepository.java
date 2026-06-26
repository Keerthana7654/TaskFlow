package com.taskflow.repository;

import com.taskflow.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Paginated tasks for a specific user with optional filters.
    // JOIN FETCH loads the User eagerly in the same query, avoiding
    // LazyInitializationException when the response DTO reads task.getUser()
    // after the request-scoped Hibernate session has closed (open-in-view=false).
    @Query("""
        SELECT t FROM Task t JOIN FETCH t.user WHERE t.user.id = :userId
        AND (:status IS NULL OR t.status = :status)
        AND (:priority IS NULL OR t.priority = :priority)
        ORDER BY t.createdAt DESC
    """)
    Page<Task> findByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("status") Task.Status status,
            @Param("priority") Task.Priority priority,
            Pageable pageable
    );

    // Admin: all tasks with optional filters (also eager-fetches user)
    @Query("""
        SELECT t FROM Task t JOIN FETCH t.user
        WHERE (:status IS NULL OR t.status = :status)
        AND (:priority IS NULL OR t.priority = :priority)
        ORDER BY t.createdAt DESC
    """)
    Page<Task> findAllWithFilters(
            @Param("status") Task.Status status,
            @Param("priority") Task.Priority priority,
            Pageable pageable
    );

    @Query("SELECT t FROM Task t JOIN FETCH t.user WHERE t.id = :id AND t.user.id = :userId")
    Optional<Task> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT t FROM Task t JOIN FETCH t.user WHERE t.id = :id")
    Optional<Task> findByIdWithUser(@Param("id") Long id);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, Task.Status status);
}
