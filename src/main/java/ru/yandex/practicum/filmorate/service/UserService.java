package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserStorage userStorage;

    public void addFriend(int userId, int friendId) {
        log.debug("Попытка добавить друга: userId={}, friendId={}", userId, friendId);
        User user = userStorage.getUserById(userId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        User friend = userStorage.getUserById(friendId).orElseThrow(() -> new NotFoundException("Друг не найден"));
        user.getFriends().add(friendId);
        friend.getFriends().add(userId);
        userStorage.updateUser(user);
        userStorage.updateUser(friend);
        log.info("Пользователь {} и {} стали друзьями", userId, friendId);
    }

    public void removeFriend(int userId, int friendId) {
        log.debug("Попытка удалить друга: userId={}, friendId={}", userId, friendId);
        User user = userStorage.getUserById(userId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        User friend = userStorage.getUserById(friendId).orElseThrow(() -> new NotFoundException("Друг не найден"));
        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);
        userStorage.updateUser(user);
        userStorage.updateUser(friend);
        log.info("Пользователь {} и {} больше не друзья", userId, friendId);
    }

    public List<User> getCommonFriends(int userId, int otherId) {
        log.debug("Поиск общих друзей у пользователей {} и {}", userId, otherId);
        User user = userStorage.getUserById(userId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        User friend = userStorage.getUserById(otherId).orElseThrow(() -> new NotFoundException("Друг не найден"));
        Set<Integer> commonIds = new HashSet<>(user.getFriends());
        return commonIds.stream()
                .map(id -> userStorage.getUserById(id).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<User> getUserFriends(int userId) {
        User user = userStorage.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
        return user.getFriends().stream()
                .map(id -> userStorage.getUserById(id).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<User> getAllUsers() {
        return userStorage.getAllUsers().stream().collect(Collectors.toList());
    }

    public User addUser(User user) {
        validateUser(user);
        checkEmailDuplicate(user.getEmail(), null);
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        User created = userStorage.addUser(user);
        log.info("Создан пользователь с id = {}", created.getId());
        return created;
    }

    public User updateUser(User user) {
        if (user.getId() == null) {
            throw new ValidationException("Id должен быть указан");
        }
        userStorage.getUserById(user.getId())
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + user.getId() + " не найден"));
        validateUser(user);
        checkEmailDuplicate(user.getEmail(), user.getId());

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        User updated = userStorage.updateUser(user);
        log.info("Обновлён пользователь с id = {}", updated.getId());
        return updated;
    }

    public User getUserById(int id) {
        return userStorage.getUserById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + id + " не найден"));
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
        boolean duplicate = userStorage.getAllUsers().stream()
                .filter(u -> !Objects.equals(u.getId(), excludeUserId))
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));

        if (duplicate) {
            throw new ValidationException("Этот email уже используется");
        }
    }
}