package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Friendship;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FriendshipStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    @Qualifier("userDbStorage")
    private final UserStorage userStorage;

    @Qualifier("friendshipDbStorage")
    private final FriendshipStorage friendshipStorage;


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

    public void addFriend(int userId, int friendId) {
        if (userId == friendId) {
            throw new ValidationException("Нельзя добавить самого себя");
        }
        userStorage.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
        userStorage.getUserById(friendId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + friendId + " не найден"));

        // Проверяем, есть ли уже прямая запись
        Optional<Friendship> existing = friendshipStorage.getByUserPair(userId, friendId);
        if (existing.isPresent()) {
            log.info("Дружба между {} и {} уже существует", userId, friendId);
        } else {
            friendshipStorage.createFriendship(userId, friendId);
            log.info("Пользователь {} добавил в друзья {}", userId, friendId);
        }
    }

    public void removeFriend(int userId, int friendId) {
        userStorage.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
        userStorage.getUserById(friendId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + friendId + " не найден"));

        friendshipStorage.deleteByUserPair(userId, friendId);
    }

    public List<User> getFriends(int userId) {
        userStorage.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));

        List<Friendship> friendships = friendshipStorage.getConfirmedFriendships(userId);
        return friendships.stream()
                .map(f -> userStorage.getUserById(f.getFriendId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<User> getCommonFriends(int userId, int otherId) {
        Set<Integer> friendsOfUser = getFriends(userId).stream().map(User::getId).collect(Collectors.toSet());
        Set<Integer> friendsOfOther = getFriends(otherId).stream().map(User::getId).collect(Collectors.toSet());
        Set<Integer> commonIds = new HashSet<>(friendsOfUser);
        commonIds.retainAll(friendsOfOther);
        return commonIds.stream()
                .map(id -> userStorage.getUserById(id).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void validateUser(User user) {
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            log.warn("Ошибка валидации: логин = {} не валидный", user.getLogin());
            throw new ValidationException("Логин должен быть указан и не должен содержать пробелы");
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