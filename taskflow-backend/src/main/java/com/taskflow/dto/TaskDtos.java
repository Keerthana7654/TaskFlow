package com.taskflow.dto;

import com.taskflow.entity.Task;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
public class TaskDtos {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class TaskRequest {
        @NotBlank(message = "Title is required")
        @Size(min = 1, max = 200, message = "Title must be 1–200 characters")
        private String title;

        @Size(max = 5000, message = "Description too long")
        private String description;

        private Task.Status status;

        private Task.Priority priority;

        private LocalDate dueDate;
    }

    @Getter @Builder
    public static class TaskResponse {
        private Long id;
        private String title;
        private String description;
        private Task.Status status;
        private Task.Priority priority;
        private LocalDate dueDate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Long userId;
        private String userName;
        private String userEmail;

        public static TaskResponse from(Task task) {
            return TaskResponse.builder()
                    .id(task.getId())
                    .title(task.getTitle())
                    .description(task.getDescription())
                    .status(task.getStatus())
                    .priority(task.getPriority())
                    .dueDate(task.getDueDate())
                    .createdAt(task.getCreatedAt())
                    .updatedAt(task.getUpdatedAt())
                    .userId(task.getUser().getId())
                    .userName(task.getUser().getName())
                    .userEmail(task.getUser().getEmail())
                    .build();
        }
    }

    @Getter @Builder
    public static class PagedTasksResponse {
        private List<TaskResponse> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean last;
    }

    @Getter @Builder
    public static class TaskStatsResponse {
        private long total;
        private long todo;
        private long inProgress;
        private long done;
        private long cancelled;
    }
}
