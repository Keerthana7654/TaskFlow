package com.taskflow.service;

import com.taskflow.dto.TaskDtos;
import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock UserRepository userRepository;
    @InjectMocks TaskService taskService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L).name("Jane").email("jane@test.com")
                .role(User.Role.USER).build();
    }

    @Test
    void createTask_withValidInput_savesAndReturnsResponse() {
        var request = new TaskDtos.TaskRequest("My task", "desc", Task.Status.TODO, Task.Priority.HIGH, null);
        var savedTask = Task.builder()
                .id(10L).title("My task").description("desc")
                .status(Task.Status.TODO).priority(Task.Priority.HIGH)
                .user(testUser).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        var response = taskService.createTask(request, 1L);

        assertThat(response.getTitle()).isEqualTo("My task");
        assertThat(response.getPriority()).isEqualTo(Task.Priority.HIGH);
        assertThat(response.getUserId()).isEqualTo(1L);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_withNonExistentUser_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        var request = new TaskDtos.TaskRequest("Task", null, null, null, null);

        assertThatThrownBy(() -> taskService.createTask(request, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMyTasks_returnsOnlyUserTasks() {
        var task = Task.builder().id(1L).title("t").status(Task.Status.TODO)
                .priority(Task.Priority.MEDIUM).user(testUser).build();
        var page = new PageImpl<>(List.of(task));

        when(taskRepository.findByUserIdWithFilters(eq(1L), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(page);

        var result = taskService.getMyTasks(1L, null, null, 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("t");
    }

    @Test
    void deleteTask_byNonOwner_throwsResourceNotFoundException() {
        when(taskRepository.findByIdAndUserId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deleteTask(5L, 2L, false))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).delete(any());
    }

    @Test
    void deleteTask_byAdmin_deletesAnyTask() {
        var task = Task.builder().id(5L).title("t").user(testUser)
                .status(Task.Status.TODO).priority(Task.Priority.LOW).build();
        when(taskRepository.findByIdWithUser(5L)).thenReturn(Optional.of(task));

        taskService.deleteTask(5L, 99L, true); // admin=true, different userId

        verify(taskRepository).delete(task);
    }

    @Test
    void updateTask_setsOnlyProvidedFields() {
        var existing = Task.builder().id(1L).title("Old title").status(Task.Status.TODO)
                .priority(Task.Priority.LOW).user(testUser).build();
        when(taskRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new TaskDtos.TaskRequest(null, null, Task.Status.DONE, null, null);
        taskService.updateTask(1L, req, 1L, false);

        assertThat(existing.getStatus()).isEqualTo(Task.Status.DONE);
        assertThat(existing.getTitle()).isEqualTo("Old title"); // unchanged
    }
}
