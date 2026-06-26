package com.taskflow.controller;

import com.taskflow.entity.User;
import com.taskflow.repository.UserRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;

    @GetMapping("/users")
    public ResponseEntity<List<UserSummary>> getAllUsers() {
        List<UserSummary> users = userRepository.findAll()
                .stream()
                .map(UserSummary::from)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserSummary> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(UserSummary::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<UserSummary> updateRole(
            @PathVariable Long id,
            @RequestBody RoleUpdateRequest request) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setRole(request.getRole());
                    return ResponseEntity.ok(UserSummary.from(userRepository.save(user)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) return ResponseEntity.notFound().build();
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Inner classes ───────────────────────────────────────────────────────

    @Getter @Builder
    public static class UserSummary {
        private Long id;
        private String name;
        private String email;
        private String role;
        private LocalDateTime createdAt;

        public static UserSummary from(User user) {
            return UserSummary.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .createdAt(user.getCreatedAt())
                    .build();
        }
    }

    @Getter
    public static class RoleUpdateRequest {
        private User.Role role;
    }
}
