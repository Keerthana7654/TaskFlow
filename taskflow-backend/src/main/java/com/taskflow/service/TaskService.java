package com.taskflow.service;

import com.taskflow.dto.TaskDtos;
import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    // ─── USER: Get their tasks ───────────────────────────────────────────────

    public TaskDtos.PagedTasksResponse getMyTasks(Long userId, Task.Status status,
                                                   Task.Priority priority, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Task> taskPage = taskRepository.findByUserIdWithFilters(userId, status, priority, pageable);
        return toPagedResponse(taskPage);
    }

    // ─── ADMIN: Get all tasks ────────────────────────────────────────────────

    public TaskDtos.PagedTasksResponse getAllTasks(Task.Status status,
                                                    Task.Priority priority, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Task> taskPage = taskRepository.findAllWithFilters(status, priority, pageable);
        return toPagedResponse(taskPage);
    }

    // ─── Get single task (user scoped) ───────────────────────────────────────

    public TaskDtos.TaskResponse getTask(Long taskId, Long userId, boolean isAdmin) {
        Task task = isAdmin
                ? taskRepository.findByIdWithUser(taskId).orElseThrow(() -> notFound(taskId))
                : taskRepository.findByIdAndUserId(taskId, userId).orElseThrow(() -> notFound(taskId));
        return TaskDtos.TaskResponse.from(task);
    }

    // ─── Create task ─────────────────────────────────────────────────────────

    @Transactional
    public TaskDtos.TaskResponse createTask(TaskDtos.TaskRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : Task.Status.TODO)
                .priority(request.getPriority() != null ? request.getPriority() : Task.Priority.MEDIUM)
                .dueDate(request.getDueDate())
                .user(user)
                .build();

        Task saved = taskRepository.save(task);
        // 'user' is the managed entity we just attached above, safe to read here
        return TaskDtos.TaskResponse.from(saved);
    }

    // ─── Update task ─────────────────────────────────────────────────────────

    @Transactional
    public TaskDtos.TaskResponse updateTask(Long taskId, TaskDtos.TaskRequest request,
                                             Long userId, boolean isAdmin) {
        Task task = isAdmin
                ? taskRepository.findByIdWithUser(taskId).orElseThrow(() -> notFound(taskId))
                : taskRepository.findByIdAndUserId(taskId, userId).orElseThrow(() -> notFound(taskId));

        if (request.getTitle() != null) task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getStatus() != null) task.setStatus(request.getStatus());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());

        return TaskDtos.TaskResponse.from(taskRepository.save(task));
    }

    // ─── Delete task ─────────────────────────────────────────────────────────

    @Transactional
    public void deleteTask(Long taskId, Long userId, boolean isAdmin) {
        Task task = isAdmin
                ? taskRepository.findByIdWithUser(taskId).orElseThrow(() -> notFound(taskId))
                : taskRepository.findByIdAndUserId(taskId, userId).orElseThrow(() -> notFound(taskId));
        taskRepository.delete(task);
    }

    // ─── Stats ───────────────────────────────────────────────────────────────

    public TaskDtos.TaskStatsResponse getStats(Long userId, boolean isAdmin) {
        long total = isAdmin ? taskRepository.count() : taskRepository.countByUserId(userId);
        long todo = isAdmin
                ? taskRepository.findAllWithFilters(Task.Status.TODO, null, PageRequest.of(0, 1)).getTotalElements()
                : taskRepository.countByUserIdAndStatus(userId, Task.Status.TODO);
        long inProgress = isAdmin
                ? taskRepository.findAllWithFilters(Task.Status.IN_PROGRESS, null, PageRequest.of(0, 1)).getTotalElements()
                : taskRepository.countByUserIdAndStatus(userId, Task.Status.IN_PROGRESS);
        long done = isAdmin
                ? taskRepository.findAllWithFilters(Task.Status.DONE, null, PageRequest.of(0, 1)).getTotalElements()
                : taskRepository.countByUserIdAndStatus(userId, Task.Status.DONE);

        return TaskDtos.TaskStatsResponse.builder()
                .total(total)
                .todo(todo)
                .inProgress(inProgress)
                .done(done)
                .cancelled(total - todo - inProgress - done)
                .build();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private TaskDtos.PagedTasksResponse toPagedResponse(Page<Task> page) {
        List<TaskDtos.TaskResponse> content = page.getContent()
                .stream()
                .map(TaskDtos.TaskResponse::from)
                .toList();

        return TaskDtos.PagedTasksResponse.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Task not found with id: " + id);
    }
}
