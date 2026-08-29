package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Friendship;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;

import java.util.List;
import java.util.Optional;

public interface FriendshipStorage {
    Friendship createFriendship(Integer userId, Integer friendId);

    Optional<Friendship> getById(Integer id);

    void deleteFriendship(Integer id);

    List<Friendship> getFriendshipsByUserId(int userId);

    Optional<Friendship> getFriendship(int userId, int friendId);

    Friendship updateStatus(Integer friendshipId, FriendshipStatus status);

    Optional<Friendship> getByUserPair(Integer userId, Integer friendId);

    List<Friendship> getSentRequests(Integer userId);

    List<Friendship> getReceivedRequests(Integer userId);

    List<Friendship> getConfirmedFriendships(Integer userId);
}