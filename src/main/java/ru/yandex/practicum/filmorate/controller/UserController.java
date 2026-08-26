package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/{id}")
    public User getUserById(@PathVariable int id) {
        log.info("Получен запрос на получение пользователя с id = {}", id);
        return userService.getUserById(id);
    }

    @GetMapping("/{id}/friends")
    public List<User> getConfirmedFriends(@PathVariable int id) {
        log.info("Запрос на получение списка подтвержденных друзей пользователя {}", id);
        return userService.getConfirmedFriends(id);
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public List<User> getCommonFriends(@PathVariable int id, @PathVariable int otherId) {
        log.info("Запрос на получение общих друзей пользователей {} и {}", id, otherId);
        return userService.getCommonFriends(id, otherId);
    }

    @GetMapping("/{id}/friends/requests/sent")
    public List<User> getSentRequests(@PathVariable int id) {
        log.info("Запрос исходящих заявок пользователя {}", id);
        return userService.getSentRequests(id);
    }

    @GetMapping("/{id}/friends/requests/received")
    public List<User> getReceivedRequests(@PathVariable int id) {
        log.info("Запрос на получение входящих заявок пользователя {}", id);
        return userService.getReceivedRequests(id);
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        log.info("Получен запрос на создание пользователя: {}", user);
        User created = userService.addUser(user);
        log.info("Пользователь успешно создан с id = {}", created.getId());
        return created;
    }

    @PutMapping
    public User update(@RequestBody User updatedUser) {
        log.info("Получен запрос на обновление пользователя с id = {}", updatedUser.getId());
        User updated = userService.updateUser(updatedUser);
        log.info("Пользователь с id = {} успешно обновлён", updated.getId());
        return updated;
    }

    @PutMapping("/{id}/friends/{friendId}")
    public void sendFriendRequest(@PathVariable int id, @PathVariable int friendId) {
        log.info("Пользователь {} добавляет в друзья {}", id, friendId);
        userService.sendFriendRequest(id, friendId);
    }

    @PutMapping("/{id}/friends/requests/{friendId}/accept")
        public void acceptFriendRequest(@PathVariable int id, @PathVariable int friendId) {
        log.info("Пользователь {} подтверждает заявку от {}", id, friendId);
        userService.acceptFriendRequest(id, friendId);
    }

    @DeleteMapping("/{id}/friends/requests/{friendId}/reject")
    public void rejectFriendRequest(@PathVariable int id, @PathVariable int friendId) {
        log.info("Пользователь {} отклоняет заявку от {}", id, friendId);
        userService.rejectFriendRequest(id, friendId);
    }
}