package com.taskflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.dto.AuthDtos;
import com.taskflow.dto.TaskDtos;
import com.taskflow.entity.Task;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired TaskRepository taskRepository;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        taskRepository.deleteAll();
        userRepository.deleteAll();
        userToken = registerAndGetToken("user@example.com", "Test@12345");
    }

    private String registerAndGetToken(String email, String password) throws Exception {
        var req = new AuthDtos.RegisterRequest("Test User", email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn();
        var resp = objectMapper.readTree(result.getResponse().getContentAsString());
        return resp.get("token").asText();
    }

    @Test
    void createTask_withValidData_returns201() throws Exception {
        var req = new TaskDtos.TaskRequest("Fix the bug", "Details here",
                Task.Status.TODO, Task.Priority.HIGH, null);

        mockMvc.perform(post("/api/tasks")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Fix the bug"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void createTask_withoutAuth_returns401() throws Exception {
        var req = new TaskDtos.TaskRequest("Fix the bug", null, null, null, null);

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createTask_withBlankTitle_returns400() throws Exception {
        var req = new TaskDtos.TaskRequest("", null, null, null, null);

        mockMvc.perform(post("/api/tasks")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void getTasks_returnsOnlyOwnTasks() throws Exception {
        // Create task as user1
        var req = new TaskDtos.TaskRequest("User1 task", null, Task.Status.TODO, Task.Priority.MEDIUM, null);
        mockMvc.perform(post("/api/tasks")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Login as user2
        String user2Token = registerAndGetToken("user2@example.com", "Test@12345");

        // User2 should see 0 tasks
        mockMvc.perform(get("/api/tasks")
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // User1 sees their 1 task — assert nested user fields resolve correctly
        // (regression guard: these fields require task.getUser() to not throw
        // LazyInitializationException once the request-scoped session is closed)
        mockMvc.perform(get("/api/tasks")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userName").value("Test User"))
                .andExpect(jsonPath("$.content[0].userEmail").value("user@example.com"));
    }

    @Test
    void updateTask_byOwner_returns200() throws Exception {
        // Create task
        var createReq = new TaskDtos.TaskRequest("Original title", null, Task.Status.TODO, Task.Priority.LOW, null);
        MvcResult created = mockMvc.perform(post("/api/tasks")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andReturn();
        Long taskId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // Update
        var updateReq = new TaskDtos.TaskRequest("Updated title", null, Task.Status.IN_PROGRESS, Task.Priority.HIGH, null);
        mockMvc.perform(put("/api/tasks/" + taskId)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void deleteTask_byOwner_returns204() throws Exception {
        var createReq = new TaskDtos.TaskRequest("To delete", null, Task.Status.TODO, Task.Priority.LOW, null);
        MvcResult created = mockMvc.perform(post("/api/tasks")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andReturn();
        Long taskId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/tasks/" + taskId)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        // Confirm gone
        mockMvc.perform(get("/api/tasks/" + taskId)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getStats_returnsCorrectCounts() throws Exception {
        var req = new TaskDtos.TaskRequest("Task 1", null, Task.Status.TODO, Task.Priority.MEDIUM, null);
        mockMvc.perform(post("/api/tasks")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks/stats")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.todo").value(1))
                .andExpect(jsonPath("$.done").value(0));
    }
}
