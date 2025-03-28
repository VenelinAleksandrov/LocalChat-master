package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.model.UserSession;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserSessionsRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionsRepository userSessionsRepository;
    @Transactional
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody User user) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> foundUser = userRepository.findByUsername(user.getUsername());

        if (foundUser.isPresent() && foundUser.get().getPassword().equals(user.getPassword())) {
            UserSession session = new UserSession();
            session.setSessionId(UUID.randomUUID().toString());
            session.setLoginTime(LocalDateTime.now());
            session.setUserId(foundUser.get().getId());
            userSessionsRepository.saveAndFlush(session);

            response.put("success", true);
            response.put("sessionId", session.getSessionId());
            response.put("avatar", foundUser.get().getAvatar());
            return ResponseEntity.ok(response);
        }

        response.put("success", false);
        response.put("message", "Invalid username or password");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {


        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Missing or invalid Authorization header"
            ));
        }

        String sessionId = authHeader.substring(7);

        try {
            Optional<UserSession> session = userSessionsRepository.findBySessionId(sessionId);

            if (session.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "success", false,
                        "message", "Session not found"
                ));
            }


            session.get().setLogoutTime(LocalDateTime.now());
            userSessionsRepository.save(session.get());

            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Logout failed: " + e.getMessage()
            ));
        }
    }
}