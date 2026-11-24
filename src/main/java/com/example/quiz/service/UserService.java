package com.example.quiz.service;

import com.example.quiz.entity.User;
import com.example.quiz.entity.enums.UserRole;
import com.example.quiz.exception.BusinessException;
import com.example.quiz.exception.ConflictException;
import com.example.quiz.exception.ResourceNotFoundException;
import com.example.quiz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", id));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Пользователь с именем '" + username + "' не найден"));
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }


    @Transactional
    public User register(String username, String email, String password) {
        username = requireText(username, "Имя пользователя");
        email = requireText(email, "Email");
        if (password == null || password.length() < 6) {
            throw new BusinessException("Пароль должен быть не короче 6 символов");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Имя '" + username + "' уже занято");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email '" + email + "' уже используется");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(UserRole.USER);
        return userRepository.save(user);
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Неверное имя или пароль"));
        if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException("Неверное имя или пароль");
        }
        return user;
    }

    public User adminLogin(String username, String password) {
        User user = login(username, password);
        if (user.getRole() != UserRole.ADMIN) {
            throw new BusinessException("У этого пользователя нет прав администратора");
        }
        return user;
    }


    @Transactional
    public User createUser(User data) {
        String username = requireText(data.getUsername(), "Имя пользователя");
        String email = requireText(data.getEmail(), "Email");

        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Имя '" + username + "' уже занято");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email '" + email + "' уже используется");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("default123"));
        user.setRole(UserRole.USER);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, User data) {
        User user = getUserById(id);

        String username = requireText(data.getUsername(), "Имя пользователя");
        String email = requireText(data.getEmail(), "Email");

        if (!username.equals(user.getUsername()) && userRepository.existsByUsername(username)) {
            throw new ConflictException("Имя '" + username + "' уже занято");
        }
        if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new ConflictException("Email '" + email + "' уже используется");
        }

        user.setUsername(username);
        user.setEmail(email);
        return user;
    }

    @Transactional
    public User changeRole(Long id, UserRole role) {
        if (role == null) {
            throw new BusinessException("Роль не указана");
        }
        User user = getUserById(id);
        user.setRole(role);
        return user;
    }


    @Transactional
    public void ensureDefaultAdmin() {
        boolean adminExists = userRepository.findAll().stream()
                .anyMatch(u -> u.getRole() == UserRole.ADMIN);
        if (adminExists) return;

        if (userRepository.existsByUsername("admin")) return;

        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@example.com");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(fieldName + " обязательно");
        }
        return value.trim();
    }
}