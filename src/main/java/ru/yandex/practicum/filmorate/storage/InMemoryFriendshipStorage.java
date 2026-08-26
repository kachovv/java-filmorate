package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Friendship;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class InMemoryFriendshipStorage implements FriendshipStorage {
    private final Map<Integer, Friendship> friendships = new HashMap<>();
    private int nextId = 1;

    @Override
    public Friendship createFriendship(Integer userId, Integer friendId) {
        Optional<Friendship> existing = getByUserPair(userId, friendId);
        if (existing.isPresent()) {
            return existing.get();
        }
        Friendship friendship = new Friendship(userId, friendId, FriendshipStatus.PENDING);
        friendship.setId(nextId++);
        friendships.put(friendship.getId(), friendship);
        return friendship;
    }

    @Override
    public Friendship updateStatus(Integer id, FriendshipStatus newStatus) {
        Friendship friendship = friendships.get(id);
        if (friendship == null) {
            throw new NotFoundException("Запись о дружбе с id " + id + " не найдена");
        }
        friendship.setStatus(newStatus);
        return friendship;
    }

    @Override
    public Optional<Friendship> getById(Integer id) {
        return Optional.ofNullable(friendships.get(id));
    }

    @Override
    public Optional<Friendship> getByUserPair(Integer userId, Integer friendId) {
        return friendships.values().stream()
                .filter(f -> (f.getUserId().equals(userId) && f.getFriendId().equals(friendId)
                || (f.getUserId().equals(friendId) && f.getFriendId().equals(userId))))
                .findFirst();
    }

    @Override
    public List<Friendship> getSentRequests(Integer userId) {
        return friendships.values().stream()
                .filter(f -> f.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Friendship> getReceivedRequests(Integer userId) {
        return friendships.values().stream()
                .filter(f -> f.getFriendId().equals(userId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Friendship> getConfirmedFriendships(Integer userId) {
        return friendships.values().stream()
                .filter(f -> (f.getUserId().equals(userId) || f.getFriendId().equals(userId))
                        && f.getStatus() == FriendshipStatus.CONFIRMED)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteFriendship(Integer id) {
        friendships.remove(id);
    }
}
