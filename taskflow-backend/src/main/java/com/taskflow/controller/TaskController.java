package com.taskflow.controller;

import com.taskflow.dto.TaskDtos;
import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<TaskDtos.PagedTasksResponse> getTasks(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Task.Status status,
            @RequestParam(required = false) Task.Priority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        TaskDtos.PagedTasksResponse response = isAdmin
                ? taskService.getAllTasks(status, priority, page, size)
                : taskService.getMyTasks(user.getId(), status, priority, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDtos.TaskResponse> getTask(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        return ResponseEntity.ok(taskService.getTask(id, user.getId(), isAdmin));
    }

    @PostMapping
    public ResponseEntity<TaskDtos.TaskResponse> createTask(
            @Valid @RequestBody TaskDtos.TaskRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(request, user.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDtos.TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskDtos.TaskRequest request,
            @AuthenticationPrincipal User user) {
        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        return ResponseEntity.ok(taskService.updateTask(id, request, user.getId(), isAdmin));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        taskService.deleteTask(id, user.getId(), isAdmin);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<TaskDtos.TaskStatsResponse> getStats(
            @AuthenticationPrincipal User user) {
        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        return ResponseEntity.ok(taskService.getStats(user.getId(), isAdmin));
    }
}
