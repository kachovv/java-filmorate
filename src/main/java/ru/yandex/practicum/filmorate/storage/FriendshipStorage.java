package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Friendship;

import java.util.List;
import java.util.Optional;

public interface FriendshipStorage {
    Friendship createFriendship(Integer userId, Integer friendId);

    Optional<Friendship> getById(Integer id);

    Optional<Friendship> getByUserPair(Integer userId, Integer friendId);

    List<Friendship> getConfirmedFriendships(Integer userId);

    void deleteByUserPair(Integer userId, Integer friendId);
}