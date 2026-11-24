package com.example.quiz.controller;

import com.example.quiz.entity.User;
import com.example.quiz.entity.enums.UserRole;
import com.example.quiz.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Page<User> getAll(@PageableDefault(size = 20) Pageable pageable) {
        return userService.getAllUsers(pageable);
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping("/by-username/{username}")
    public User getByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User create(@RequestBody User user) {
        return userService.createUser(user);
    }

    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user);
    }

    @PatchMapping("/{id}/role")
    public User changeRole(@PathVariable Long id, @RequestParam UserRole role) {
        return userService.changeRole(id, role);
    }
}