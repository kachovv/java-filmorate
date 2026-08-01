package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {
    private final Map<Integer, User> users = new HashMap<>();
    private int nextId = 1;

    @GetMapping
    public Collection<User> findAll() {
        return users.values();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        log.info("Получен запрос на создание пользователя: {}", user);
        validateUser(user);
        checkEmailDuplicate(user.getEmail(), null);

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName((user.getLogin()));
        }
        user.setId(nextId++);
        users.put(user.getId(), user);
        log.info("Пользователь успешно создан с id = {}", user.getId());
        return user;
    }

    @PutMapping
    public User update(@RequestBody User updatedUser) {
        log.info(("Получен запрос на обновление пользоватея с id = {}"), updatedUser.getId());
        if (updatedUser.getId() == null) {
            log.warn("Попытка обновления без id");
            throw new ValidationException("Id должен быть указан");
        }
        if (!users.containsKey(updatedUser.getId())) {
            log.warn("Пользователь с id = {} не найден", updatedUser.getId());
            throw new NotFoundException("Пользователь с id = " + updatedUser.getId() + " не найден");
        }
        validateUser(updatedUser);
        checkEmailDuplicate(updatedUser.getEmail(), updatedUser.getId());

        User existing = users.get(updatedUser.getId());
        existing.setEmail(updatedUser.getEmail());
        existing.setLogin(updatedUser.getLogin());

        if (updatedUser.getName() == null || updatedUser.getName().isBlank()) {
            existing.setName(updatedUser.getLogin());
        } else {
            existing.setName(updatedUser.getName());
        }
        existing.setBirthday(updatedUser.getBirthday());
        log.info("Пользователь с id = {} успешно обновлен", existing.getId());
        return existing;
    }

    private void validateUser(User user) {
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            log.warn("Ошибка валидации: логин = {} не валидный", user.getLogin());
            throw new ValidationException(("Логин должен быть указан и не должен содержать пробелы"));
        }
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.warn("Ошибка валидации: email = {} не валидный", user.getEmail());
            throw new ValidationException("Email должен быть указан и содержать @");
        }
        if (user.getBirthday() == null || user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Ошибка валидации: дата рождения {} в будущем", user.getBirthday());
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }

    private void checkEmailDuplicate(String email, Integer excludeUserId) {
        boolean duplicate = users.values().stream()
                .filter(u -> !Objects.equals(u.getId(), excludeUserId))
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));

        if (duplicate) {
            throw new ValidationException("Этот email уже используется");
        }
    }
}