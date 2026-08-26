package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Friendship;

import java.util.List;
import java.util.Optional;

public interface FriendshipStorage {
    Friendship createFriendship(Integer userId, Integer friendId);

    Optional<Friendship> getById(Integer id);

    void deleteFriendship(Integer id);

    List<Friendship> getFriendshipsByUserId(int userId);

    Optional<Friendship> getFriendship(int userId, int friendId);
}