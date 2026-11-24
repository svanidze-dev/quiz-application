package com.example.quiz.controller;

import com.example.quiz.entity.User;
import com.example.quiz.entity.enums.UserRole;
import com.example.quiz.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    public record RegisterRequest(String username, String email, String password) {}
    public record LoginRequest(String username, String password) {}

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public User register(@RequestBody RegisterRequest r, HttpServletRequest req) {
        User user = userService.register(r.username(), r.email(), r.password());
        HttpSession session = req.getSession(true);
        session.setAttribute("userId", user.getId());
        return user;
    }

    @PostMapping("/login")
    public User login(@RequestBody LoginRequest r, HttpServletRequest req) {
        User user = userService.login(r.username(), r.password());
        HttpSession session = req.getSession(true);
        session.setAttribute("userId", user.getId());
        return user;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return ResponseEntity.status(401).body(Map.of("error", "not logged in"));
        }

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "not logged in"));
        }

        try {
            User user = userService.getUserById(userId);
            boolean isAdmin = (user.getRole() == UserRole.ADMIN);

            return ResponseEntity.ok(Map.of(
                    "user", user,
                    "isAdmin", isAdmin
            ));
        } catch (Exception e) {
            session.invalidate();
            return ResponseEntity.status(401).body(Map.of("error", "user not found"));
        }
    }
}